import type { NavigatorScreenParams } from '@react-navigation/native';

export type AuthStackParams = { Login: undefined; Signup: undefined; Server: undefined };

export type CustomerTabParams = { Home: undefined; MyJobs: undefined; Account: undefined };
export type CustomerStackParams = {
  Tabs: NavigatorScreenParams<CustomerTabParams> | undefined;
  Category: { code: string; name: string };
  PostJob: { itemCode: string; itemName: string; categoryCode: string };
  JobDetail: { jobId: number };
  BulkPost: undefined;
  BulkDetail: { bulkJobId: number };
  Server: undefined;
};

export type LabourTabParams = { Home: undefined; Bulk: undefined; Profile: undefined; Account: undefined };
export type LabourStackParams = { Tabs: NavigatorScreenParams<LabourTabParams> | undefined; Kyc: undefined; Server: undefined };

export type AdminStackParams = { KycReview: undefined; Server: undefined };
