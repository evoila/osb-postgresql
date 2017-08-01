import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'sb-auto-scaler',
  templateUrl: './auto-scaler.component.html',
  styleUrls: ['./auto-scaler.component.scss']
})
export class AutoScalerComponent implements OnInit {
  configuration  = {
    minMaxRange: [1, 10],
    cpuLowerUpperLimit: [1, 100],
    ramLowerUpperLimit: [1, 100],
    scalingEnabled: true,
    scalingIntervalMultiplier: 1,
    cooldownTime: 30,
    learningEnabled: true,
    learningTimeMultiplier: 5,
    cpuThresholdPolicy: 20,
    quotientBasedScalingEnabled: true,
    minQuotient: 1,
    ramThresholdPolicy: 30,
    billingIntervalConsidered: false,
    requestThresholdPolicy: 40
  };
  range = 10;
  minMaxConfig: any = {
    behaviour: 'drag',
    connect: true,
    margin: 1,
    step: 1,
    range: {
      min: 1,
      max: 25
    },
    pips: {
      mode: 'steps',
      density: 1
    }
  };
  lowerUpperConfig: any = {
    behaviour: 'drag',
    connect: true,
    margin: 5,
    step: 5,
    range: {
      min: 0,
      max: 100
    },
    pips: {
      mode: 'steps',
      density: 5
    }
  };

  constructor() { }

  ngOnInit() {}

}
