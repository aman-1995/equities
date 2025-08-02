import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EquityPositionsComponent } from './components/equity-positions/equity-positions.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, EquityPositionsComponent],
  template: `
    <app-equity-positions></app-equity-positions>
  `,
  styles: []
})
export class AppComponent {
  title = 'equities-ui';
}
