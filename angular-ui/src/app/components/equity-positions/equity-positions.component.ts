import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { Subscription } from 'rxjs';
import { PositionService } from '../../services/position.service';
import { Transaction, Position } from '../../models/transaction.model';

@Component({
  selector: 'app-equity-positions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    InputNumberModule,
    SelectModule,
    TableModule,
    ProgressSpinnerModule,
    TooltipModule,
    MessageModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './equity-positions.component.html',
  styleUrls: ['./equity-positions.component.scss']
})
export class EquityPositionsComponent implements OnInit, OnDestroy {
  positions: Position[] = [];
  transactions: Transaction[] = [];
  newTransaction: Transaction = {
    tradeId: 0,
    version: 1,
    securityCode: '',
    quantity: 0,
    action: 'INSERT',
    side: 'BUY'
  };
  bulkTransactionsText = '';
  isLoading = false;
  errorMessage = '';
  backendStatus = 'Unknown';
  isEditing = false;
  editingTransaction: Transaction | null = null;

  displayedColumns: string[] = ['transactionId', 'tradeId', 'version', 'securityCode', 'quantity', 'action', 'side', 'actions'];

  actionOptions = [
    { label: 'INSERT', value: 'INSERT' },
    { label: 'UPDATE', value: 'UPDATE' },
    { label: 'CANCEL', value: 'CANCEL' }
  ];

  sideOptions = [
    { label: 'Buy', value: 'BUY' },
    { label: 'Sell', value: 'SELL' }
  ];

  private subscriptions = new Subscription();

  constructor(
    private positionService: PositionService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(
      this.positionService.positions$.subscribe(positions => {
        this.positions = positions;
      })
    );
    this.subscriptions.add(
      this.positionService.transactions$.subscribe(transactions => {
        this.transactions = transactions;
      })
    );
    this.checkBackendHealth();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private checkBackendHealth(): void {
    this.positionService.checkHealth().subscribe({
      next: () => {
        this.backendStatus = 'Connected';
        this.errorMessage = '';
      },
      error: () => {
        this.backendStatus = 'Disconnected';
        this.errorMessage = 'Backend service is not available.';
      }
    });
  }

  addTransaction(): void {
    if (!this.validateTransaction(this.newTransaction)) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.positionService.processTransaction({ ...this.newTransaction }).subscribe({
      next: () => {
        this.resetForm();
        this.isLoading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Transaction added successfully'
        });
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: error.message || 'Failed to add transaction'
        });
      }
    });
  }

  processBulkTransactions(): void {
    if (!this.bulkTransactionsText.trim()) {
      this.errorMessage = 'Please enter transactions.';
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'Please enter transactions.'
      });
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const transactions = this.parseBulkTransactions(this.bulkTransactionsText);
    if (transactions.length === 0) {
      this.errorMessage = 'No valid transactions found.';
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'No valid transactions found.'
      });
      this.isLoading = false;
      return;
    }

    this.positionService.processBulkTransactions(transactions).subscribe({
      next: () => {
        this.bulkTransactionsText = '';
        this.isLoading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `${transactions.length} transactions processed successfully`
        });
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: error.message || 'Failed to process bulk transactions'
        });
      }
    });
  }

  clearAll(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.positionService.clearAll().subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
      }
    });
  }

  loadSampleData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.positionService.loadSampleData().subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
      }
    });
  }

  formatPosition(quantity: number): string {
    return quantity > 0 ? `+${quantity}` : quantity.toString();
  }

  editTransaction(transaction: Transaction): void {
    if (!this.isLatestTransaction(transaction)) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Cannot Edit',
        detail: 'Only the latest transaction version for each trade can be edited.'
      });
      return;
    }
    
    this.isEditing = true;
    this.editingTransaction = { ...transaction };
    this.newTransaction = { ...transaction };
  }

  isLatestTransaction(transaction: Transaction): boolean {
    if (transaction.isLatestVersion !== undefined) {
      return transaction.isLatestVersion;
    }
    return false;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.editingTransaction = null;
    this.resetForm();
  }

  updateTransaction(): void {
    if (!this.validateTransaction(this.newTransaction)) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.positionService.processTransaction({ ...this.newTransaction }).subscribe({
      next: () => {
        this.isEditing = false;
        this.editingTransaction = null;
        this.resetForm();
        this.isLoading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Transaction updated successfully'
        });
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: error.message || 'Failed to update transaction'
        });
      }
    });
  }

  private resetForm(): void {
    this.newTransaction = {
      tradeId: 0,
      version: 1,
      securityCode: '',
      quantity: 0,
      action: 'INSERT',
      side: 'BUY'
    };
  }

  private validateTransaction(transaction: Transaction): boolean {
    // Transaction ID is optional for new transactions, required for updates
    if (this.isEditing && (!transaction.transactionId || transaction.transactionId <= 0)) {
      this.errorMessage = 'Transaction ID must be a positive number.';
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Transaction ID must be a positive number.'
      });
      return false;
    }

    if (!transaction.tradeId || transaction.tradeId <= 0) {
      this.errorMessage = 'Trade ID must be a positive number.';
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Trade ID must be a positive number.'
      });
      return false;
    }

    if (!transaction.version || transaction.version <= 0) {
      this.errorMessage = 'Version must be a positive number.';
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Version must be a positive number.'
      });
      return false;
    }

    if (!transaction.securityCode || transaction.securityCode.trim() === '') {
      this.errorMessage = 'Security code is required.';
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Security code is required.'
      });
      return false;
    }

    if (!transaction.quantity || transaction.quantity <= 0) {
      this.errorMessage = 'Quantity must be a positive number.';
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Quantity must be a positive number.'
      });
      return false;
    }

    this.errorMessage = '';
    return true;
  }

  private parseBulkTransactions(text: string): Transaction[] {
    const lines = text.trim().split('\n');
    const transactions: Transaction[] = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (!line) continue;

      const parts = line.split(',').map(part => part.trim());
      if (parts.length < 7) continue;

      try {
        const transaction: Transaction = {
          transactionId: parts[0] ? parseInt(parts[0]) : undefined,
          tradeId: parseInt(parts[1]),
          version: parseInt(parts[2]),
          securityCode: parts[3],
          quantity: parseInt(parts[4]),
          action: parts[5] as 'INSERT' | 'UPDATE' | 'CANCEL',
          side: parts[6] as 'BUY' | 'SELL'
        };

        if (this.validateTransaction(transaction)) {
          transactions.push(transaction);
        }
      } catch (error) {
      }
    }

    return transactions;
  }
} 