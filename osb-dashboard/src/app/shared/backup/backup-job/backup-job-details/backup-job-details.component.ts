import { Component, OnInit } from '@angular/core';
import { BackupService } from '../../backup.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Job } from '../../domain/job';

@Component({
  selector: 'sb-backup-job-details',
  templateUrl: './backup-job-details.component.html',
  styleUrls: ['./backup-job-details.component.scss']
})
export class BackupJobDetailsComponent implements OnInit {
  job: Job | any = {};

  constructor(protected readonly backupService: BackupService,
              protected readonly route: ActivatedRoute,
              protected readonly router: Router) { }

 ngOnInit(): void {
     this.route.params.subscribe(params => {
       if (params['jobId']) {
          this.backupService.loadOne('jobs', params['jobId'])
            .subscribe(
              (job: any) => {this.job = job},
            );
       }
    });
}

  public delete() {
    console.log('DELETE');
    this.backupService.deleteOne('jobs', this.job.getId()).subscribe((jobs: any) => {
        this.router.navigate(['/backup']);
    });
  }
}
