import { Component } from '@angular/core';

import { environment } from 'environments/runtime-environment';

import {
  BuildTargetService, ModuleSupport
} from 'app/shared';

@Component({
  selector: 'sb-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  public logoSrc = environment.ui.logoSrc;
  public readonly moduleSupport: ModuleSupport;
  public readonly dynamicModuleSupport: any;

  title = 'app works!';

  constructor(
    buildTarget: BuildTargetService
  ) {
    this.dynamicModuleSupport = buildTarget.dynamicModuleSupport;
    this.moduleSupport = buildTarget.moduleSupport;
  }
}
