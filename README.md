
# react-native-fitness
`react-native-fitness` is a library that works on both `iOS` and `Android` with it you can interact with Apple Healthkit and Google Fit.
Currently the lib provides a set of [API](#API) that you can use to read steps count or distance count for a given period of time.

Note:
We're open to receive PRs that contains new features or improvements, so feel free to contribute to this repo.

## Getting started

`npm install @ovalmoney/react-native-fitness --save`

or

`yarn add @ovalmoney/react-native-fitness`

### Mostly automatic installation

`$ react-native link @ovalmoney/react-native-fitness`

### Manual installation


#### iOS

### Pods
1. Add the line below to your podfile then run `pod install`
`pod 'react-native-fitness', :path => '../node_modules/@ovalmoney/react-native-fitness'`
2. In XCode, in the project navigator, select your project. Add `libreact-native-fitness.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
3. Add `NSHealthShareUsageDescription` to your `Info.plist` in order to ask permissions.

### Manually

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-fitness` and add `RNFitness.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNFitness.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

In order to make it run, it is necessary to turn on `Health Kit` in the `Capabilities`.

#### Android
1. Get an OAuth 2.0 Client ID as explained at https://developers.google.com/fit/android/get-api-key
2. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.oval.fitness.RNFitnessPackage;` to the imports at the top of the file
  - Add `new RNFitnessPackage()` to the list returned by the `getPackages()` method
3. Append the following lines to `android/settings.gradle`:
  	```
  	include ':@ovalmoney_react-native-fitness'
  	project(':@ovalmoney_react-native-fitness').projectDir = new File(rootProject.projectDir, 	'../node_modules/@ovalmoney/react-native-fitness/android')
  	```
4. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':@ovalmoney_react-native-fitness')
  	```


## Usage

```javascript
import Fitness from '@ovalmoney/react-native-fitness';

Fitness.isAuthorized(permissions: { kind: Fitness.PermissionKind.Steps, access: Fitness.PermissionAccess.Write })
  .then((authorized) => {
    //Do something
  })
  .catch((error) => {
    //Do something
  });
```
### API

- **Fitness.isAuthorized(permissions: [{ kind: int, access: int }])**
Check if permissions are granted or not. It works on Android and iOS >= 12.0, while it returns an error when iOS < 12.
It requires an `Array` of `Object` with a mandatory key `kind` and an optional key `access`.
Possible values for the keys can be found in `PermissionKind` and `PermissionAccess` under `Attributes` section.
On iOS at least one permissions with `Read` access must be provided, otherwise an `errorEmptyPermissions` will be thrown.

- **Fitness.requestPermissions(permissions: [{ kind: int, access: int }])**
Ask permission and return if user granted or not(Android), while, due to Apple's privacy model, always true is returned in iOS.
It requires an `Array` of `Object` with a mandatory key `kind` and an optional key `access`.
Possible values for the keys can be found in `PermissionKind` and `PermissionAccess` under `Attributes` section.
On iOS at least one permissions with `Read` access must be provided, otherwise an `errorEmptyPermissions` will be thrown.

- **Fitness.getSteps(dates: { startDate: string, endDate: string, interval: string })**
Fetch steps on a given period of time. It requires an `Object` with `startDate` and `endDate` attributes as string. If startDate is not provided an error will be thrown. Set `interval` to decide how detailed the returned data is, set it to `hour` otherwise it defaults to `days`.

- **Fitness.getDistance(dates: { startDate: string, endDate: string, interval: string })**
Fetch distance in meters on a given period of time. It requires an `Object` with `startDate` and `endDate` attributes as string. If startDate is not provided an error will be thrown. Set `interval` to decide how detailed the returned data is, set it to `hour` otherwise it defaults to `days`.

- **Fitness.getCalories(dates: { startDate: string, endDate: string, interval: string })**
Fetch calories burnt in kilocalories on a given period of time. It requires an `Object` with `startDate` and `endDate` attributes as string. If startDate is not provided an error will be thrown. Set `interval` to decide how detailed the returned data is, set it to `hour` otherwise it defaults to `days`.

- **Fitness.getHeartRate(dates: { startDate: string, endDate: string, interval: string })**
Fetch heart rate bpm on a given period of time. It requires an `Object` with `startDate` and `endDate` attributes as string. If startDate is not provided an error will be thrown. Set `interval` to decide how detailed the returned data is, set it to `hour` otherwise it defaults to `days`.

- **Fitness.subscribeToActivity()**
Available only on android. Subscribe to all Google Fit activities. It returns a promise with `true` for a successful subscription and `false` otherwise.
Call this function to get all google fit activites and eliminate the need to have Google Fit installed on the device. 

- **Fitness.subscribeToSteps()**
Available only on android. Subscribe only to steps from the Google Fit store. It returns a promise with `true` for a successful subscription and `false` otherwise.
Call this function to get steps and eliminate the need to have Google Fit installed on the device.

### Attributes

#### Platform
Return the used provider.

#### PermissionKind
Return the information of what kind of Permission can be asked.
At the moment the list of possible kinds is:
 - ***Step***: to required the access for `Step`
 - ***Distances***: to required the access for `Distances`
 - ***Calories***: to required the access for `Calories`
 - ***HeartRate***: to required the access for `Heart rate`
 - ***Activity***: to required the access for `Activity` (only Android)

#### PermissionAccess
Return the information of what kind of Access can be asked.
The list of possible kinds is:
 - ***Read***: to required the access to `Read`
 - ***Write***: to required the access to `Write`

#### Error (iOS only)
Return the list of meaningful errors that can be possible thrown.
On Android it is an empty object.
Possible values are:
 - ***hkNotAvailable***: thrown if HealthKit is not available
 - ***methodNotAvailable***: thrown if ```isAuthorized``` is called on iOS < 12.0
 -  ***dateNotCorrect***: thrown if received date is not correct
 - ***errorEmptyPermissions***: thrown if no read permissions are provided
 - ***errorNoEvents***: thrown if an error occurs while try to retrieve data


