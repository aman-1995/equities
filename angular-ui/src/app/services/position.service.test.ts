import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PositionService } from './position.service';
import { Transaction, Position } from '../models/transaction.model';

describe('PositionService', () => {
  let service: PositionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PositionService]
    });
    service = TestBed.inject(PositionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created and initialize with empty data', () => {
    expect(service).toBeTruthy();
    
    const req1 = httpMock.expectOne('/api');
    req1.flush([]);
    const req2 = httpMock.expectOne('/api/transactions');
    req2.flush([]);
    
    expect(service.getPositions()).toEqual([]);
    expect(service.getTransactions()).toEqual([]);
  });

  it('should process transaction via HTTP API', () => {
    const testTransaction: Transaction = {
      transactionId: 1,
      tradeId: 1,
      version: 1,
      securityCode: 'REL',
      quantity: 50,
      action: 'INSERT',
      side: 'BUY'
    };

    const mockPositions: Position[] = [
      { securityCode: 'REL', quantity: 50 }
    ];

    const req1 = httpMock.expectOne('/api');
    req1.flush([]);
    const req2 = httpMock.expectOne('/api/transactions');
    req2.flush([]);

    service.processTransaction(testTransaction).subscribe(positions => {
      expect(positions).toEqual(mockPositions);
    });

    const req = httpMock.expectOne('/api/transaction');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(testTransaction);
    req.flush(mockPositions);

    const req3 = httpMock.expectOne('/api/transactions');
    req3.flush([]);
  });
}); 