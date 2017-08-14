import { Component, OnInit } from '@angular/core';
import { ServiceKeysService } from './service-keys.service';

@Component({
  selector: 'sb-service-keys',
  templateUrl: './service-keys.component.html',
  styleUrls: ['./service-keys.component.scss']
})
export class ServiceKeysComponent implements OnInit {
  readonly ENTITY = 'servicekeys';
  serviceKeys: [any];
  isLoading = false;

  constructor(protected readonly service: ServiceKeysService) { }

  ngOnInit() {
    this.loadKeys();
  }

  loadKeys(): void {
    this.service.loadAll(this.ENTITY)
    .subscribe((keys_page: any) => {
      this.serviceKeys = keys_page.content;
    });
  }

  create(): void {
    this.isLoading = true;
    this.service.saveOne({}, this.ENTITY)
      .subscribe((key: any) => {
        this.isLoading = false;
        this.loadKeys();
      });
  }
}
