import paymentBankOptionSource from './paymentBankOptions.json';

export interface PaymentBankLimit {
  label: string;
  value: string;
}

export interface PaymentBankOption {
  code: string;
  name: string;
  shortName: string;
  limits: PaymentBankLimit[];
}

export const paymentBankOptions: PaymentBankOption[] = paymentBankOptionSource;
