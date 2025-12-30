import { initializeApp, getApps } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
    apiKey: "AIzaSyD_buKSjyXoOS-8zJrNZSumKlz0iF5Kseo",
    authDomain: "ioritv-70318.firebaseapp.com",
    projectId: "ioritv-70318",
    storageBucket: "ioritv-70318.appspot.com",
    messagingSenderId: "836647250439",
    appId: "1:836647250439:web:f78397202368895a7f98a2",
    measurementId: "G-JHR8YM5FPB"
};

// Initialize Firebase
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];
const auth = getAuth(app);
const db = getFirestore(app);

export { app, auth, db };
