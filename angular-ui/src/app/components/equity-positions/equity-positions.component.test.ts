import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { EquityPositionsComponent } from './equity-positions.component';
import { PositionService } from '../../services/position.service';
import { Transaction } from '../../models/transaction.model';

describe('EquityPositionsComponent', () => {
  let component: EquityPositionsComponent;
  let fixture: ComponentFixture<EquityPositionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EquityPositionsComponent, HttpClientTestingModule],
      providers: [PositionService, MessageService]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EquityPositionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('isLatestTransaction', () => {
    it('should return false for transaction without transactionId', () => {
      const transaction: Transaction = {
        tradeId: 1,
        version: 1,
        securityCode: 'REL',
        quantity: 50,
        action: 'INSERT',
        side: 'BUY'
      };

      expect(component.isLatestTransaction(transaction)).toBe(false);
    });

    it('should use backend-provided isLatestVersion flag when available', () => {
      const transaction: Transaction = {
        transactionId: 1,
        tradeId: 1,
        version: 1,
        securityCode: 'REL',
        quantity: 50,
        action: 'INSERT',
        side: 'BUY',
        isLatestVersion: true
      };

      expect(component.isLatestTransaction(transaction)).toBe(true);
    });

    it('should fall back to local calculation when isLatestVersion is not provided', () => {
      const transactions: Transaction[] = [
        {
          transactionId: 1,
          tradeId: 1,
          version: 1,
          securityCode: 'REL',
          quantity: 50,
          action: 'INSERT',
          side: 'BUY'
        },
        {
          transactionId: 2,
          tradeId: 1,
          version: 2,
          securityCode: 'REL',
          quantity: 60,
          action: 'UPDATE',
          side: 'BUY'
        }
      ];

      component.transactions = transactions;

      // Version 2 should be the latest
      expect(component.isLatestTransaction(transactions[1])).toBe(true);
      // Version 1 should not be the latest
      expect(component.isLatestTransaction(transactions[0])).toBe(false);
    });

    it('should return true for single transaction of a trade', () => {
      const transaction: Transaction = {
        transactionId: 1,
        tradeId: 1,
        version: 1,
        securityCode: 'REL',
        quantity: 50,
        action: 'INSERT',
        side: 'BUY'
      };

      component.transactions = [transaction];

      expect(component.isLatestTransaction(transaction)).toBe(true);
    });

    it('should handle multiple trades correctly', () => {
      const transactions: Transaction[] = [
        {
          transactionId: 1,
          tradeId: 1,
          version: 1,
          securityCode: 'REL',
          quantity: 50,
          action: 'INSERT',
          side: 'BUY'
        },
        {
          transactionId: 2,
          tradeId: 1,
          version: 2,
          securityCode: 'REL',
          quantity: 60,
          action: 'UPDATE',
          side: 'BUY'
        },
        {
          transactionId: 3,
          tradeId: 2,
          version: 1,
          securityCode: 'ITC',
          quantity: 40,
          action: 'INSERT',
          side: 'SELL'
        }
      ];

      component.transactions = transactions;

      // Trade 1, Version 2 should be latest
      expect(component.isLatestTransaction(transactions[1])).toBe(true);
      // Trade 1, Version 1 should not be latest
      expect(component.isLatestTransaction(transactions[0])).toBe(false);
      // Trade 2, Version 1 should be latest (only transaction for that trade)
      expect(component.isLatestTransaction(transactions[2])).toBe(true);
    });
  });
}); 