import { Component, OnInit } from '@angular/core';
import { BackupService } from '../backup.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'sb-file-endpoint',
  templateUrl: './file-endpoint.component.html',
  styleUrls: ['./file-endpoint.component.scss']
})
export class FileEndpointComponent implements OnInit {
  readonly ENTITY: string = 'destinations';
  destination: any = {};
  update = false;
  validated = false;
  submitLabel = 'Validate';

  constructor(protected readonly backupService: BackupService,
              protected readonly route: ActivatedRoute,
              protected readonly router: Router) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      console.log(params['fileEndpointId']);
       if (params['fileEndpointId']) {
         this.update = true;
          this.backupService.loadOne(this.ENTITY, params['fileEndpointId'])
            .subscribe(
              (destination: any) => { this.destination = destination},
            );
       }
    });

  }

  delete(): void {
    this.backupService.deleteOne(this.ENTITY, this.destination)
      .subscribe((destination: any) => {
        this.redirect();
      });
  }

  onSubmit(): void {
    if (!this.validated) {
      this.backupService.validate(this.ENTITY, this.destination)
        .subscribe((destination: any) => {
          this.validated = true;
          this.submitLabel = 'Submit';
        });
    } else {
      const id = this.update ? this.destination.id : null;
      this.backupService.saveOne(this.destination, this.ENTITY, id)
        .subscribe((plan: any) => {
          this.redirect();
      });
    }
  }

  private redirect(): void {
    this.router.navigate(['/backup']);
  }

}
