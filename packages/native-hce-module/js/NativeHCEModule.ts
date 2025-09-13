import type {TurboModule, CodegenTypes} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export type HCEModuleEventType = 'sessionStarted' | 'readerDetected' | 'readerDeselected' | 'received' | 'sessionInvalidated';
export type HCEModuleStopReason = 'success' | 'failure';

export type HCEModuleEvent = {
  type: HCEModuleEventType
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
  stopHCE(status: HCEModuleStopReason): Promise<void>;
  respondAPDU(rapdu: string): Promise<void>;
  isHCERunning(): Promise<boolean>;

  readonly onEvent: CodegenTypes.EventEmitter<HCEModuleEvent>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'NativeHCEModule',
);
