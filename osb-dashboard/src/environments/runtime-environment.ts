// this var may be injected via index.html and server-side include
const injectedEnv = window['INJECTED_ENVIRONMENT'];
if (!injectedEnv) {
  // tslint:disable-next-line:no-console
  console.warn('DASHBOARD DEFAULT DEVELOPMENT ENV ACTIVE - DID YOU FORGET TO INJECT A RUNTIME ENV?');
}

export interface Environment {
  production: boolean;
  baseUrls: { serviceBrokerUrl: string };
  auth: {
    tokenRenewalPreFresh: {
      primary: number, // min
      session: number // min
    },
    // this setting allows us to simulate auth token epiration by invalidating tokens after a timeout
    simulatedTokenInvalidationTimeout?: false // ms
  };
  ui: {
    title: string;
    logoSrc: string;
  };
};

// we use quoutes here because that makes it easier to copy config to nginx.conf or cf manifest files
// tslint:disable:quotemark
const seedEnv = {
  production: false,
  baseUrls: {
    serviceBrokerUrl: "https://this.service-broker.io", // do not commit changes to this value, it is needed in CI
  },
  auth: {
    tokenRenewalPreFresh: {
      primary: 2, // min
      session: 2 // min
    },
    // this setting allows us to simulate auth token epiration by invalidating tokens after a timeout
    simulatedTokenInvalidationTimeout: false // ms
  },
  ui: {
    title: "Service Broker Panel",
    logoSrc: "./assets/core/sb-white.svg"
  }
};

// overwrite default env with injected vars
export const environment: Environment = Object.assign({}, seedEnv, injectedEnv);
