import { Component, OnInit } from '@angular/core';
import { BackupService } from './backup.service';
import { Job } from './domain/job';
import { BackupPlan } from './domain/backup-plan';
import { FileEndpoint } from './domain/file-endpoint';

@Component({
  selector: 'sb-backup',
  templateUrl: './backup.component.html',
  styleUrls: ['./backup.component.scss']
})
export class BackupComponent implements OnInit {
  jobs: Job[];
  plans: BackupPlan[];
  destinations: FileEndpoint[];

  constructor(protected readonly backupService: BackupService) { }

  ngOnInit() {
    this.backupService
      .loadAll('plans')
      .subscribe((plans: any) => {
        this.plans = plans.content;
      });

    this.backupService
      .loadAll('jobs')
      .subscribe((jobs: any) => {
        this.jobs = jobs.content;
      });

    this.backupService
      .loadAll('destinations')
      .subscribe((destinations: any) => {
        this.destinations = destinations.content;
      })
  }

  startBackup(id: string) {
    this.backupService.saveOne({destinationId: id}, 'backup')
      .subscribe((job: any) => {
      });
  }
}
