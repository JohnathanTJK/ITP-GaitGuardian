# 🦶 GaitGuardian
GaitGuardian is a mobile application designed to make tracking the progression of Parkinson’s disease more accessible by using automated gait analysis powered by machine learning.

## 🔐 Key Features

- **All-in-one assessment tool** Record videos and perform Timed Up and Go (TUG) gait analysis in a single app.
- **Patient and Clinician Functionalities** Switch between Clinician and Patient Views with role-specific functionalities.
- **Subtask Segmentation** Automatically break down the TUG test into subtasks and measure the time spent on each.
- **Video Privacy Control** Patients can choose whether to save or discard recorded videos, ensuring privacy.
---
## ▶️ How It Works
- Patient can record video recordings of them doing TUG tests.
- GaitGuardian will run video analysis through a local Flask server.
- Output generated from the video analysis will be shown to the Patient, indicating the severity rating and other relevant metrics. Generated response will be stored in the database.
- Clinician can use GaitGuardian to access Patient's past assessments and review them.
---
## 🛠️ How to Run GaitGuardian
_Please ensure that the Flask server and mobile application is connected to the **same** network._

### Install Requirements and Run Flask Server

```bash
cd computervision_test
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```
### Change the IP Address in GaitGuardianAnalysis.kt (line 19)
```bash
// REPLACE THIS IP WITH YOUR COMPUTER'S IP ADDRESS
private const val BASE_URL = "http://<<YOUR IP HERE>>:5001/"  // Change this IP!
```

Make sure to rebuild the GaitGuardian app using Android Studio after updating the IP address so the changes take effect.


## 🗂️ Project Structure
### File Structure of Pose Detection and Machine Learning Backend
```
computervision_test
├── model/
│   ├── xgb_features.json               ← Pose Feature columns for XGBoost model input
│   ├── xgb_label_encoder.pkl           ← LabelEncoder for TUG subtask labels
│   └── xgb_model.pkl                   ← XGBoost model
│
├── app.py/                             ← Flask API for gait analysis: pose extraction, subtask classification, gait metrics, and severity scoring
├── requirements.txt                    ← Required Modules for the backend
```
###### Please refer to the [`integration`](https://github.com/JohnathanTJK/ITP-GaitGuardian/tree/integration) branch for ML/Pose Estimation Testing codes.
### File Structure of GaitGuardian Android App
```
app/src/main/java/com/example/gaitguardian
├── api/                                      
│   ├── GaitAnalysisAPI.kt              ← Retrofit API interface
│   ├── GaitAnalysisClient.kt           ← API client management (Retrofit builder, etc.)
│   ├── GaitAnalysisModels.kt           ← Data models for gait analysis API
│   └── TestApiConnection.kt            ← API connection testing utility
│
├── data/                               ← Data Layer
│   ├── roomDatabase/                  
│   │   ├── clinician/                  ← Clinician entity, Clinician DAO, and repository
│   │   ├── patient/                    ← Patient entity, Patient DAO, and repository
│   │   └── tug/                        ← TUG entity DAO, TUG Analysis entity DAO, and repository
│   ├── GaitGuardianDatabase.kt         ← Database setup and instance provider
│   └── sharedPreferences/             
│       └── AppPreferencesRepository.kt ← SharedPreferences handler
│
├── screens/                            ← View Layer
│   ├── camera/                         ← Camera capture and preview screens
│   ├── clinician/                      ← Home, PIN entry, assessment details, performance graphs
│   ├── patient/                        ← Home, recording, loading, results
│   ├── SettingsScreen.kt                   ← Screen to manage app settings
│   ├── SplashScreen.kt                     ← Initial loading/splash screen
│   └── StartScreen.kt                      ← Welcome screen for new users
│
├── viewmodels/                         ← ViewModel Layer
│   ├── ClinicianViewModel.kt           ← Manages clinician-related UI state
│   ├── PatientViewModel.kt             ← Manages patient-related UI state
│   └── TugDataViewModel.kt             ← Manages TUG assessment data
│
├── GaitGuardian.kt                     ← Application class
├── MainActivity.kt                     ← Entry point activity with navigation host
└── NavGraph.kt                         ← Centralized navigation graph
```

---

## 📞	 Contact
**GaitGuardian Team Members:**
- **AARON TE** - 2301970@sit.singaporetech.edu.sg
- **BOO YAN CONG** - 2302238@sit.singaporetech.edu.sg
- **CHNG YU QI BERNICE** - 2302020@sit.singaporetech.edu.sg
- **SITI NURHASYIMAH BINTE MOHD EBRAHIM** - 2302151@sit.singaporetech.edu.sg
- **TOH JUN KUAN JOHNATHAN** - 2301915@sit.singaporetech.edu.sg
