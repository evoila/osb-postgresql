import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BackupComponent } from './backup/backup.component';
import { ServiceKeysComponent } from './service-keys/service-keys.component';
import { NoContentComponent } from './no-content/no-content.component';
import { RouterModule } from '@angular/router';

const components = [BackupComponent, ServiceKeysComponent, NoContentComponent]

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    RouterModule
  ],
  declarations: [...components],
  exports: [components]
})
export class SharedModule { }
