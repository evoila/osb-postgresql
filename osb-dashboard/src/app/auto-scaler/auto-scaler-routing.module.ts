import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AutoScalerComponent } from './auto-scaler.component';


export const ROUTES = [
  {
    path: '',
    component: AutoScalerComponent,
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forChild(ROUTES)],
  exports: [RouterModule]
})
export class AutoScalerRoutingModule { }
