export type MangoStatusTone = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export interface MangoStatusTextProps {
  tone?: MangoStatusTone;
}
