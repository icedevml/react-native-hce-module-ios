import Foundation
import CoreNFC

extension StringProtocol {
  func asHexArray() -> [UInt8] {
    var startIndex = self.startIndex
    return stride(from: 0, to: count, by: 2).compactMap { _ in
      let endIndex = index(startIndex, offsetBy: 2, limitedBy: self.endIndex) ?? self.endIndex
      defer { startIndex = endIndex }
      return UInt8(self[startIndex..<endIndex], radix: 16)
    }
  }
}

extension Data {
  func hexEncodedString() -> String {
    let format = "%02hhX"
    return map { String(format: format, $0) }.joined()
  }
}

@available(iOS 17.4, *)
@objc public class RTNHCEModuleIOSImpl: NSObject {
  var cardSession: CardSession? = nil
  var cardSessionInvalidated: Bool = true
  var presentmentIntent: NFCPresentmentIntentAssertion? = nil
  var receivedCardAPDU: CardSession.APDU? = nil

  var emitOnEvent: ((NSString, NSString?) -> ())? = nil

  @objc public func setEmitOnEvent(emitOnEvent: @escaping (NSString, NSString?) -> ()) {
    self.emitOnEvent = emitOnEvent
  }

  @objc public func acquireExclusiveNFC(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    Task.init {
      if self.presentmentIntent != nil && self.presentmentIntent!.isValid {
        reject("err_already_exists", "NFCPresentmentIntentAssertion already exists and is valid.", nil)
        return
      }

      self.presentmentIntent = nil

      do {
        self.presentmentIntent = try await NFCPresentmentIntentAssertion.acquire()
      } catch (let error) {
        // TODO exact error details here and in other places as well
        reject("err_create_presentment", "Failed to create NFCPresentmentIntentAssertion: \(error)", nil)
        return
      }

      resolve(nil)
    }
  }

  @objc public func isExclusiveNFC() -> Bool {
    return self.presentmentIntent != nil && self.presentmentIntent!.isValid
  }

  @objc public func beginSession(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    Task.init {
      guard NFCReaderSession.readingAvailable else {
        reject("err_nfc_reader_not_available", "Preflight check failed: NFCReaderSession.readingAvailable is false", nil)
        return
      }

      guard CardSession.isSupported else {
        reject("err_card_session_unsupported", "Preflight check failed: CardSession.isSupported is false", nil)
        return
      }

      guard await CardSession.isEligible else {
        reject("err_card_session_not_eligible", "Preflight check failed: CardSession.isEligible is false", nil)
        return
      }

      guard self.cardSessionInvalidated else {
        reject("err_card_session_exists", "CardSession already exists.", nil)
        return
      }

      do {
        self.cardSession = try await CardSession()
      } catch (let error) {
        reject("err_create_card_session", "Failed to create CardSession(): \(error)", nil)
        return
      }

      resolve(nil)

      do {
        for try await event in cardSession!.eventStream {
          switch event {
          case .sessionStarted:
            cardSession!.alertMessage = String(localized: "Communicating with card reader.")
            self.emitOnEvent!("sessionStarted", "")

          case .readerDetected:
            self.emitOnEvent!("readerDetected", "")

          case .readerDeselected:
            self.emitOnEvent!("readerDeselected", "")

          case .received(let cardAPDU):
            self.receivedCardAPDU = cardAPDU
            self.emitOnEvent!("received", NSString(string:cardAPDU.payload.hexEncodedString()))

          case .sessionInvalidated(let reason):
            self.cardSessionInvalidated = true
            self.emitOnEvent!("sessionInvalidated", "\(reason)" as NSString)
            self.cardSession = nil

          @unknown default:
            fatalError("Received unknown CardSession event.")
          }
        }
      } catch (let err) {
        self.emitOnEvent!("internalError", "\(err)" as NSString)
      }
    }
  }

  @objc public func setSessionAlertMessage(message: NSString) {
    self.cardSession?.alertMessage = String(message)
  }

  @objc public func invalidateSession() {
    self.cardSession?.invalidate()
    self.cardSession = nil
    self.cardSessionInvalidated = true
  }

  @objc public func isSessionRunning() -> Bool {
    return self.cardSession != nil && !self.cardSessionInvalidated
  }

  @objc public func isHCERunning(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    Task.init {
      guard self.cardSession != nil else {
        return resolve(false)
      }

      let isRunning = await self.cardSession!.isEmulationInProgress
      resolve(isRunning)
    }
  }

  @objc public func respondAPDU(rapdu: NSString, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    Task.init {
      guard let capdu = self.receivedCardAPDU else {
        reject("err_no_apdu_received", "There is no received APDU to respond to.", nil)
        return
      }

      do {
        try await capdu.respond(response: Data.init(String(rapdu).asHexArray()))
      } catch (let error) {
        reject("err_apdu_respond", "Error trying to respond to APDU. \(error)", nil)
        return
      }

      resolve(nil)
    }
  }

  @objc public func startHCE(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    Task.init {
      do {
        try await self.cardSession?.startEmulation()
      } catch(let error) {
        reject("err_start_emulation", "Error trying to start emulation. \(error)", nil)
        return
      }

      resolve(nil)
    }
  }

  @objc public func stopHCE(status: NSString, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    Task.init {
      let sStatus = String(status)

      if (sStatus == "success") {
        await self.cardSession?.stopEmulation(status: .success)
      } else {
        await self.cardSession?.stopEmulation(status: .failure)
      }

      resolve(nil)
    }
  }
}
