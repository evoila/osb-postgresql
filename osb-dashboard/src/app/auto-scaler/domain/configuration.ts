export interface Configuration {
  // Multiplicator of Scaling checks multiplied with 30s
  scalingIntervalMultiplier: number;

  // Minimum required instance starting from 0
  minInstances: number;

  // Maximum required instances > minInstances
  maxInstances: number;

  minMaxRange: number[];

  // Threshhold time, during the scaler is not allowed to scale
  cooldownTime: number;

  // Timeout until learned behavouir shall be applied
  learningTimeMultiplier: number;

  cpuUpperLimit: number;

  cpuLowerLimit: number;

  cpuLowerUpperLimit: number[];

  // Do know yet
  minQuotient: number;

  ramUpperLimit: number;

  ramLowerLimit: number;

  ramLowerUpperLimit: number[];

  scalingEnabled: boolean;

  quotientBasedScalingEnabled: boolean;

  learningEnabled: boolean;

  billingIntervalConsidered: boolean;

  // ("max", "min", "mean")
  cpuThresholdPolicy: string;

  // ("max", "min", "mean")
  requestThresholdPolicy: string;

  // ("max", "min", "mean")
  ramThresholdPolicy: string
}
