import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';
import { PositionService } from '../../services/position.service';
import { Transaction, Position } from '../../models/transaction.model';

@Component({
  selector: 'app-equity-positions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './equity-positions.component.html',
  styleUrls: ['./equity-positions.component.scss']
})
export class EquityPositionsComponent implements OnInit, OnDestroy {
  positions: Position[] = [];
  transactions: Transaction[] = [];
  newTransaction: Transaction = {
    transactionId: 0,
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

  displayedColumns: string[] = ['transactionId', 'tradeId', 'version', 'securityCode', 'quantity', 'action', 'side'];

  private subscriptions = new Subscription();

  constructor(private positionService: PositionService) {}

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
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
      }
    });
  }

  processBulkTransactions(): void {
    if (!this.bulkTransactionsText.trim()) {
      this.errorMessage = 'Please enter transactions.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const transactions = this.parseBulkTransactions(this.bulkTransactionsText);
    if (transactions.length === 0) {
      this.errorMessage = 'No valid transactions found.';
      return;
    }

    this.positionService.processBulkTransactions(transactions).subscribe({
      next: () => {
        this.bulkTransactionsText = '';
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = `Error: ${error.message || 'Unknown error'}`;
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

  private resetForm(): void {
    this.newTransaction = {
      transactionId: 0,
      tradeId: 0,
      version: 1,
      securityCode: '',
      quantity: 0,
      action: 'INSERT',
      side: 'BUY'
    };
  }

  private validateTransaction(transaction: Transaction): boolean {
    if (!transaction.transactionId || transaction.transactionId <= 0) {
      this.errorMessage = 'Transaction ID must be a positive number.';
      return false;
    }

    if (!transaction.tradeId || transaction.tradeId <= 0) {
      this.errorMessage = 'Trade ID must be a positive number.';
      return false;
    }

    if (!transaction.version || transaction.version <= 0) {
      this.errorMessage = 'Version must be a positive number.';
      return false;
    }

    if (!transaction.securityCode || transaction.securityCode.trim() === '') {
      this.errorMessage = 'Security code is required.';
      return false;
    }

    if (!transaction.quantity || transaction.quantity <= 0) {
      this.errorMessage = 'Quantity must be a positive number.';
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
          transactionId: parseInt(parts[0]),
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