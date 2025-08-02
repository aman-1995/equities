import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Transaction, Position } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class PositionService {
  private readonly API_BASE_URL = '/api';

  private positionsSubject = new BehaviorSubject<Position[]>([]);
  private transactionsSubject = new BehaviorSubject<Transaction[]>([]);

  public positions$ = this.positionsSubject.asObservable();
  public transactions$ = this.transactionsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadInitialData();
  }

  private loadInitialData(): void {
    this.getAllPositions().subscribe();
    this.getAllTransactions().subscribe();
  }

  processTransaction(transaction: Transaction): Observable<Position[]> {
    return this.http.post<Position[]>(`${this.API_BASE_URL}/transaction`, transaction)
      .pipe(
        tap(positions => {
          this.positionsSubject.next(positions);
          this.getAllTransactions().subscribe();
        })
      );
  }

  processBulkTransactions(transactions: Transaction[]): Observable<Position[]> {
    return this.http.post<Position[]>(`${this.API_BASE_URL}/transactions/bulk`, transactions)
      .pipe(
        tap(positions => {
          this.positionsSubject.next(positions);
          this.getAllTransactions().subscribe();
        })
      );
  }

  processBulkTransactionsAsync(transactions: Transaction[]): Observable<Position[]> {
    return this.http.post<Position[]>(`${this.API_BASE_URL}/transactions/bulk-async`, transactions)
      .pipe(
        tap(positions => {
          this.positionsSubject.next(positions);
          this.getAllTransactions().subscribe();
        })
      );
  }

  getAllPositions(): Observable<Position[]> {
    return this.http.get<Position[]>(`${this.API_BASE_URL}`)
      .pipe(
        tap(positions => this.positionsSubject.next(positions))
      );
  }

  getAllTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.API_BASE_URL}/transactions`)
      .pipe(
        tap(transactions => this.transactionsSubject.next(transactions))
      );
  }

  clearAll(): Observable<void> {
    return this.http.delete<void>(`${this.API_BASE_URL}/clear`)
      .pipe(
        tap(() => {
          this.positionsSubject.next([]);
          this.transactionsSubject.next([]);
        })
      );
  }

  loadSampleData(): Observable<Position[]> {
    return this.http.post<Position[]>(`${this.API_BASE_URL}/load-sample-data`, {})
      .pipe(
        tap(positions => {
          this.positionsSubject.next(positions);
          this.getAllTransactions().subscribe();
        })
      );
  }

  getPositions(): Position[] {
    return this.positionsSubject.value;
  }

  getTransactions(): Transaction[] {
    return this.transactionsSubject.value;
  }

  checkHealth(): Observable<any> {
    return this.http.get(`${this.API_BASE_URL}/health`, { responseType : 'arraybuffer' });
  }
} 