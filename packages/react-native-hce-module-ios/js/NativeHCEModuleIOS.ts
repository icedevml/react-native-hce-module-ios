import type {TurboModule, CodegenTypes} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export type HCEModuleIOSEventType = 'sessionStarted' | 'readerDetected' | 'readerDeselected' | 'received' | 'sessionInvalidated';
export type HCEModuleIOSStopReason = 'success' | 'failure';

export type HCEModuleIOSEvent = {
  type: HCEModuleIOSEventType
  arg: string | null,
}

export interface Spec extends TurboModule {
  isPlatformSupported(): boolean;

  acquireExclusiveNFC(): Promise<boolean>;
  isExclusiveNFC(): boolean;

  beginSession(): Promise<void>;
  setSessionAlertMessage(message: string): void;
  invalidateSession(): void;
  isSessionRunning(): boolean;

  startHCE(): Promise<void>;
  stopHCE(status: HCEModuleIOSStopReason): Promise<void>;
  respondAPDU(rapdu: string): Promise<void>;
  isHCERunning(): Promise<boolean>;

  readonly onEvent: CodegenTypes.EventEmitter<HCEModuleIOSEvent>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'NativeHCEModuleIOS',
);
