import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import React from 'react';
import { Text } from 'react-native';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { SignupScreen } from '../screens/auth/SignupScreen';
import { AccountScreen } from '../screens/common/AccountScreen';
import { ServerScreen } from '../screens/common/ServerScreen';
import { BulkDetailScreen } from '../screens/customer/BulkDetailScreen';
import { BulkPostScreen } from '../screens/customer/BulkPostScreen';
import { CategoryScreen } from '../screens/customer/CategoryScreen';
import { CustomerHomeScreen } from '../screens/customer/HomeScreen';
import { JobDetailScreen } from '../screens/customer/JobDetailScreen';
import { MyJobsScreen } from '../screens/customer/MyJobsScreen';
import { PostJobScreen } from '../screens/customer/PostJobScreen';
import { KycReviewScreen } from '../screens/admin/KycReviewScreen';
import { BulkFeedScreen } from '../screens/labour/BulkFeedScreen';
import { LabourHomeScreen } from '../screens/labour/HomeScreen';
import { KycScreen } from '../screens/labour/KycScreen';
import { LabourProfileScreen } from '../screens/labour/ProfileScreen';
import { useAuth } from '../state/AuthContext';
import { colors } from '../theme';
import type {
  AdminStackParams, AuthStackParams, CustomerStackParams, CustomerTabParams, LabourStackParams, LabourTabParams,
} from './types';

const header = { headerStyle: { backgroundColor: colors.navy }, headerTintColor: '#fff', headerTitleStyle: { fontWeight: '700' as const } };
const icon = (glyph: string) => () => <Text style={{ fontSize: 18 }}>{glyph}</Text>;
const tabOpts = { ...header, tabBarActiveTintColor: colors.blue };

const AuthStack = createNativeStackNavigator<AuthStackParams>();
function AuthNavigator() {
  return (
    <AuthStack.Navigator screenOptions={header}>
      <AuthStack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
      <AuthStack.Screen name="Signup" component={SignupScreen} options={{ title: 'Create account' }} />
      <AuthStack.Screen name="Server" component={ServerScreen} options={{ title: 'Server settings' }} />
    </AuthStack.Navigator>
  );
}

const CTabs = createBottomTabNavigator<CustomerTabParams>();
function CustomerTabs() {
  return (
    <CTabs.Navigator screenOptions={tabOpts}>
      <CTabs.Screen name="Home" component={CustomerHomeScreen} options={{ title: 'WorkNear', tabBarLabel: 'Home', tabBarIcon: icon('🏠') }} />
      <CTabs.Screen name="MyJobs" component={MyJobsScreen} options={{ title: 'My jobs', tabBarIcon: icon('📋') }} />
      <CTabs.Screen name="Account" component={AccountScreen} options={{ tabBarIcon: icon('👤') }} />
    </CTabs.Navigator>
  );
}
const CStack = createNativeStackNavigator<CustomerStackParams>();
function CustomerNavigator() {
  return (
    <CStack.Navigator screenOptions={header}>
      <CStack.Screen name="Tabs" component={CustomerTabs} options={{ headerShown: false }} />
      <CStack.Screen name="Category" component={CategoryScreen} options={({ route }) => ({ title: route.params.name })} />
      <CStack.Screen name="PostJob" component={PostJobScreen} options={{ title: 'Post a job' }} />
      <CStack.Screen name="JobDetail" component={JobDetailScreen} options={{ title: 'Job' }} />
      <CStack.Screen name="BulkPost" component={BulkPostScreen} options={{ title: 'Bulk requirement' }} />
      <CStack.Screen name="BulkDetail" component={BulkDetailScreen} options={{ title: 'Bulk requirement' }} />
      <CStack.Screen name="Server" component={ServerScreen} options={{ title: 'Server settings' }} />
    </CStack.Navigator>
  );
}

const LTabs = createBottomTabNavigator<LabourTabParams>();
function LabourTabs() {
  return (
    <LTabs.Navigator screenOptions={tabOpts}>
      <LTabs.Screen name="Home" component={LabourHomeScreen} options={{ title: 'Jobs', tabBarIcon: icon('🛠️') }} />
      <LTabs.Screen name="Bulk" component={BulkFeedScreen} options={{ title: 'Group jobs', tabBarIcon: icon('👷') }} />
      <LTabs.Screen name="Profile" component={LabourProfileScreen} options={{ tabBarIcon: icon('📝') }} />
      <LTabs.Screen name="Account" component={AccountScreen} options={{ tabBarIcon: icon('👤') }} />
    </LTabs.Navigator>
  );
}
const LStack = createNativeStackNavigator<LabourStackParams>();
function LabourNavigator() {
  return (
    <LStack.Navigator screenOptions={header}>
      <LStack.Screen name="Tabs" component={LabourTabs} options={{ headerShown: false }} />
      <LStack.Screen name="Kyc" component={KycScreen} options={{ title: 'KYC documents' }} />
      <LStack.Screen name="Server" component={ServerScreen} options={{ title: 'Server settings' }} />
    </LStack.Navigator>
  );
}

const AStack = createNativeStackNavigator<AdminStackParams>();
function AdminNavigator() {
  return (
    <AStack.Navigator screenOptions={header}>
      <AStack.Screen name="KycReview" component={KycReviewScreen} options={{ title: 'KYC review (admin)' }} />
      <AStack.Screen name="Server" component={ServerScreen} options={{ title: 'Server settings' }} />
    </AStack.Navigator>
  );
}

export function RootNavigator() {
  const { session } = useAuth();
  if (!session) return <AuthNavigator />;
  switch (session.userType) {
    case 'CUSTOMER': return <CustomerNavigator />;
    case 'LABOUR': return <LabourNavigator />;
    case 'ADMIN': return <AdminNavigator />;
  }
}
