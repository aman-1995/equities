import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { EquityPositionsComponent } from './equity-positions.component';
import { PositionService } from '../../services/position.service';
import { Transaction } from '../../models/transaction.model';

describe('EquityPositionsComponent', () => {
  let component: EquityPositionsComponent;
  let fixture: ComponentFixture<EquityPositionsComponent>;
  let positionService: PositionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        EquityPositionsComponent, 
        FormsModule, 
        HttpClientTestingModule,
        NoopAnimationsModule
      ],
      providers: [PositionService]
    }).compileComponents();

    fixture = TestBed.createComponent(EquityPositionsComponent);
    component = fixture.componentInstance;
    positionService = TestBed.inject(PositionService);
    fixture.detectChanges();
  });

  afterEach(() => {
    if (component) {
      component.ngOnDestroy();
    }
  });

  it('should create component and initialize with empty data', () => {
    expect(component).toBeTruthy();
    expect(component.positions.length).toBe(0);
    expect(component.transactions.length).toBe(0);
    expect(component.newTransaction.transactionId).toBe(0);
    expect(component.newTransaction.securityCode).toBe('');
    expect(component.newTransaction.side).toBe('BUY');
  });

  it('should process a valid transaction successfully', () => {
    const spy = jest.spyOn(positionService, 'processTransaction').mockReturnValue({
      subscribe: (callbacks: any) => {
        callbacks.next([]);
        return { unsubscribe: () => {} };
      }
    } as any);
    
    const testTransaction: Transaction = {
      transactionId: 1,
      tradeId: 1,
      version: 1,
      securityCode: 'REL',
      quantity: 50,
      action: 'INSERT',
      side: 'BUY'
    };
    
    component.newTransaction = { ...testTransaction };
    component.addTransaction();

    expect(spy).toHaveBeenCalledWith(testTransaction);
    expect(component.newTransaction.transactionId).toBe(0);
  });
}); 