# 🦶 GaitGuardian
GaitGuardian is a mobile application designed to make tracking the progression of Parkinson’s disease more accessible by using automated gait analysis powered by machine learning.

## 🔐 Key Features

- **All-in-one assessment tool** Record videos and perform Timed Up and Go (TUG) gait analysis in a single app.
- **Patient and Clinician Functionalities** Switch between Clinician and Patient Views with role-specific functionalities.
- **On-Device Video Analysis** Perform Video Analysis without an external server.
- **Subtask Segmentation** Automatically break down the TUG test into subtasks and measure the time spent on each.
- **Video Privacy Control** Patients can choose whether to save or discard recorded videos, ensuring privacy.
---
## ▶️ How It Works
- Patient can record video recordings of them doing TUG tests.
- GaitGuardian will run video analysis through an on-device pipeline.
- Output generated from the video analysis will be shown to the Patient, indicating the severity rating and other relevant metrics. Generated response will be stored in the database.
- Clinician can use GaitGuardian to access Patient's past assessments and review them.
- Clincian also have access to an interactive Video Playback UI where they can watch assessments tagged with the specific subtask being performed at various timestamps on both portrait and landscape view.
---
## 🛠️ How to Run GaitGuardian
Please ensure that you have Android Studio installed.
Sync Gradle Files if required
Build the application and run it on the emulator.

## 🗂️ Project Structure
### File Structure of Pose Detection and Machine Learning Backend
```
itp2compvision+phaseclassification
├── model/
│   ├── xgb_features.json               ← Pose Feature columns for XGBoost model input
│   ├── xgb_label_encoder.pkl           ← LabelEncoder for TUG subtask labels
│   └── xgb_model.pkl                   ← XGBoost model
│
├── app.py/                             ← Flask API for gait analysis: pose extraction, subtask classification, gait metrics, and severity scoring
├── requirements.txt                    ← Required Modules for the backend

itp2mlp
├── model/
│   ├── xgb_features.json               ← Pose Feature columns for XGBoost model input
│   ├── xgb_label_encoder.pkl           ← LabelEncoder for TUG subtask labels
│   └── xgb_model.pkl                   ← XGBoost model
│
├── app.py/                             ← Flask API for gait analysis: pose extraction, subtask classification, gait metrics, and severity scoring
├── requirements.txt                    ← Required Modules for the backend
```
### File Structure of GaitGuardian Android App
```
app/src/main/java/com/example/gaitguardian
├── api/                                      
│   ├── GaitAnalysisAPI.kt              ← Retrofit API interface (remove? since its unused?)
│   ├── GaitAnalysisClient.kt           ← API client management (Retrofit builder, etc.)
│   ├── GaitAnalysisModels.kt           ← Data models for gait analysis API
│   └── TestApiConnection.kt            ← API connection testing utility
│
├── data/                               ← Data Layer
│   ├── models/                         
│   │   ├── TugResult                   ← Generate a data class to store video analysis result
│   ├── roomDatabase/                  
│   │   ├── clinician/                  ← Clinician entity, Clinician DAO, and repository
│   │   ├── patient/                    ← Patient entity, Patient DAO, and repository
│   │   └── tug/                        ← TUG entity DAO, TUG Analysis entity DAO, and repository
│   ├── GaitGuardianDatabase.kt         ← Database setup and instance provider
│   └── sharedPreferences/             
│       └── AppPreferencesRepository.kt ← SharedPreferences handler
│
├── screens/                            ← View Layer
│   ├── camera/                         ← Camera capture and UI Overlay
│   ├── clinician/                      ← Home, PIN entry, assessment details, performance graphs, video playback
│   ├── patient/                        ← Home, recording, loading, results, how-to-use guides
│   ├── SettingsScreen.kt               ← Screen to manage app settings
│   ├── SplashScreen.kt                 ← Initial loading/splash screen
│   └── StartScreen.kt                  ← Welcome screen for new users
│
├── viewmodels/                         ← ViewModel Layer
│   ├── ClinicianViewModel.kt           ← Manages clinician-related UI state
│   ├── PatientViewModel.kt             ← Manages patient-related UI state
│   └── TugDataViewModel.kt             ← Manages TUG assessment data
│   └── CameraViewModel.kt              ← Manages Camera-related UI state
│
└── FeatureExtraction.kt                ← Extract Features (angle rotation, movement complexity)
├── GaitGuardian.kt                     ← Application class
├── MainActivity.kt                     ← Entry point activity with navigation host
└── NotificationService.kt              ← Notification broadcast setup
└── PoseExtraction.kt                   ← Process body landmarks
└── SeverityClassification.kt           ← Classify severity 
└── SeverityPrediction.kt               ← Predict severity
└── TugPrediction.kt                    ← Predict TUG 

```

---

## 📞	 Contact
**GaitGuardian Team Members:**
- **AARON TE** - 2301970@sit.singaporetech.edu.sg
- **BOO YAN CONG** - 2302238@sit.singaporetech.edu.sg
- **CHNG YU QI BERNICE** - 2302020@sit.singaporetech.edu.sg
- **SITI NURHASYIMAH BINTE MOHD EBRAHIM** - 2302151@sit.singaporetech.edu.sg
- **TOH JUN KUAN JOHNATHAN** - 2301915@sit.singaporetech.edu.sg
