export interface Transaction {
  transactionId: number;
  tradeId: number;
  version: number;
  securityCode: string;
  quantity: number;
  action: 'INSERT' | 'UPDATE' | 'CANCEL';
  side: 'BUY' | 'SELL';
}

export interface Position {
  securityCode: string;
  quantity: number;
}

export interface Trade {
  tradeId: number;
  transactions: Transaction[];
} 