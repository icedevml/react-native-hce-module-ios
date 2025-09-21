#import "RTNHCEModuleIOS.h"
#import "RCTDefaultReactNativeFactoryDelegate.h"
#import "RTNHCEModuleIOS-Swift.h"

@implementation RTNHCEModuleIOS {
    API_AVAILABLE(ios(17.4))
    RTNHCEModuleIOSImpl *hcemoduleios;
}

-(id) init {
    self = [super init];

    if (@available(iOS 17.4, *)) {
        __weak RTNHCEModuleIOS *weakSelf = self;

        hcemoduleios = [RTNHCEModuleIOSImpl new];
        [hcemoduleios setEmitOnEventWithEmitOnEvent:^(NSString *type, NSString *arg){
            [weakSelf emitOnEvent:@{@"type": type, @"arg": arg}];
        }];
    } else {
        // ignore
    }

    return(self);
}

- (void)dealloc {
    [hcemoduleios setEmitOnEventWithEmitOnEvent:^(NSString *type, NSString *arg){
        // no-op
    }];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeHCEModuleSpecJSI>(params);
}

- (nonnull NSNumber *)isPlatformSupported {
    if (@available(iOS 17.4, *)) {
        return @1;
    }

    return @0;
}

- (void)acquireExclusiveNFC:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios acquireExclusiveNFCWithResolve:resolve reject:reject];
    } else {
        reject(@"err_platform_unsupported", @"This iOS version does not support HCE.", nil);
    }
}

- (void)releaseExclusiveNFC {
    if (@available(iOS 17.4, *)) {
        [hcemoduleios releaseExclusiveNFC];
    }
}

- (nonnull NSNumber *)isExclusiveNFC {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios isExclusiveNFC] ? @1 : @0;
    }

    return @0;
}

- (void)beginSession:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios beginSessionWithResolve:resolve reject:reject];
    } else {
        reject(@"err_platform_unsupported", @"This iOS version does not support HCE.", nil);
    }
}

- (void)setSessionAlertMessage:(nonnull NSString *)message {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios setSessionAlertMessageWithMessage:message];
    }
}

- (void)invalidateSession {
    if (@available(iOS 17.4, *)) {
        [hcemoduleios invalidateSession];
    }
}

- (nonnull NSNumber *)isSessionRunning {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios isSessionRunning] ? @1 : @0;
    } else {
        return @0;
    }
}

- (nonnull NSNumber *)beginBackgroundHCE:(nonnull NSString *)handle {
    return @0;
}

- (nonnull NSNumber *)finishBackgroundHCE:(nonnull NSString *)handle {
    return @0;
}

- (void)startHCE:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios startHCEWithResolve:resolve reject:reject];
    } else {
        reject(@"err_platform_unsupported", @"This iOS version does not support HCE.", nil);
    }
}

- (void)stopHCE:(nonnull NSString *)status resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios stopHCEWithStatus:status resolve:resolve reject:reject];
    } else {
        reject(@"err_platform_unsupported", @"This iOS version does not support HCE.", nil);
    }
}

- (void)respondAPDU:(NSString * _Nullable)handle rapdu:(nonnull NSString *)rapdu resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    if (@available(iOS 17.4, *)) {
        return [hcemoduleios respondAPDUWithHandle:handle rapdu:rapdu resolve:resolve reject:reject];
    } else {
        reject(@"err_platform_unsupported", @"This iOS version does not support HCE.", nil);
    }
}

- (void)isHCERunning:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    if (@available(iOS 17.4, *)) {
        [hcemoduleios isHCERunningWithResolve:resolve reject:reject];
    } else {
        reject(@"err_platform_unsupported", @"This iOS version does not support HCE.", nil);
    }
}

+ (NSString *)moduleName {
    return @"NativeHCEModule";
}

@end
