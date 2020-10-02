package com.example.androidsecondproject.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.androidsecondproject.model.Chat;
import com.example.androidsecondproject.model.ChatAndMessages;
import com.example.androidsecondproject.model.Match;
import com.example.androidsecondproject.model.Message;
import com.example.androidsecondproject.model.Preferences;
import com.example.androidsecondproject.model.Profile;
import com.example.androidsecondproject.model.Question;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Repository {

    private final String PROFILE_TABLE = "profiles";
    private final String QUESTIONS_TABLE = "questions_table";
    private final String CHATS_TABLE = "chats_table";
    private FirebaseDatabase database;
    private AuthRepository authRepository;
    private StorageRepository storageRepository;
    private DatabaseReference profilesTable;
    private DatabaseReference questionsTable;
    private DatabaseReference chatsTable;
    private ProfileListener profileListener;
    private ProfilesListener profilesListener;
    private  MessageListener messageListener;
    private ChatListener chatListener;
    private ProfilesForGuestListener profilesForGuestListener;

    private static Repository repository;
    private QuestionsListener questionsListener;
    private ReadOtherProfileListener readOtherProfileListener;
    private MatchesListener matchesListener;


    private Repository(Context context) {
        database=FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(false);
        profilesTable=database.getReference(PROFILE_TABLE);
        questionsTable=database.getReference(QUESTIONS_TABLE);
        chatsTable = database.getReference(CHATS_TABLE);
        chatsTable.keepSynced(true);
        authRepository=AuthRepository.getInstance(context);
        storageRepository=StorageRepository.getInstance();

    }
    public static Repository getInstance(Context context){
        if(repository==null){
            repository=new Repository(context);
        }
        return repository;
    }



    public void readProfile(String uid){

                profilesTable.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                                Profile profile=snapshot.getValue(Profile.class);
                            Log.d("prof","tst2");
                                if(profileListener!=null) {
                                    Log.d("prof", "tst3");
                                    profileListener.onProfileDataChangeSuccess(profile);
                                }
                        }
                        else {
                            if(profileListener!=null)
                                profileListener.onProfileDataChangeFail("not_exist");
                        }
                    }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                        if(profileListener!=null)
                            profileListener.onProfileDataChangeFail(error.getMessage());
                        //TODO
                    }
        });

    }

    public void readProfiles(final Profile myProfile){

        profilesTable.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){
                    String myUid=getCurrentUserId();
                    List<Profile> profiles=new ArrayList<>();
                    for(DataSnapshot currSnapshot:snapshot.getChildren()){
                       if(!currSnapshot.getKey().equals(myUid)) {
                           Profile profile = currSnapshot.getValue(Profile.class);
                           if(checkCompatibility(myProfile,profile)) {
                               profiles.add(profile);

                           }
                       }
                    }
                    profilesListener.onProfilesDataChangeSuccess(profiles);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void readMatches(final Profile myProfile){

        profilesTable.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){
                    String myUid=getCurrentUserId();
                    List<Profile> profiles=new ArrayList<>();
                    List<Match> matches=myProfile.getMatches();
                    for(DataSnapshot currSnapshot:snapshot.getChildren()){
                        String otherUid=currSnapshot.getKey();
                        Log.d("uid",otherUid);
                        if(!otherUid.equals(myUid)) {
                            Profile profile = currSnapshot.getValue(Profile.class);
                            for(Match match:matches){
                                if(match.getOtherUid().equals(otherUid))
                                    profiles.add(profile);
                            }
                            ;
                        }
                    }
                    Log.d("size",profiles.size()+"");
                    matchesListener.onMatchesDataChangeSuccess(profiles);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private boolean checkCompatibility(Profile myProfile, Profile otherProfile) {
        if(!otherProfile.isDiscovery()){
            return false;
        }
        if(checkCompatibilityHelper(myProfile,otherProfile)&&checkCompatibilityHelper(otherProfile,myProfile)){
            return true;
        }
        return false;
    }





    private boolean checkCompatibilityHelper(Profile profile, Profile otherProfile) {
        float myAge = profile.getAge();
        String myGender = profile.getGender();
        Preferences otherPrefences = otherProfile.getPreferences();
        Preferences myPreferences=profile.getPreferences();

        if (myAge < otherPrefences.getMinAge() || myAge > otherPrefences.getMaxAge()) {
            return false;
        } else if (!((otherPrefences.isLookingForMen() && myGender.equals("male")) || (otherPrefences.isLookingForWomen() && myGender.equals("female")))) {
            return false;
        } else if (profile.getCity() == null && otherProfile.getCity() != null) {
            return false;
        }
        else if(profile.getLocation()!=null&&otherProfile.getLocation()!=null){
            Log.d("dist",profile.getLocation().calculateDistance(otherProfile.getLocation())+"");
            if(profile.getLocation().calculateDistance(otherProfile.getLocation())>myPreferences.getMaxDistance())
                return false;
        }
        return true;
    }

    public void writeMyProfile(Profile profile){
        Log.d("prof","tst1");
        profilesTable.child(authRepository.getCurrentUserUid()).setValue(profile);
        //TODO
    }
    public void writeOtherProfile(Profile profile){
        profilesTable.child((profile.getUid())).setValue(profile);
    }
    public void updateProfile(String uid, String key, Object objectToUpdate){
        Map<String,Object> map =new HashMap<>();
        map.put(key,objectToUpdate);
        profilesTable.child((uid)).updateChildren(map);
    }
  /*  public void updateSpecificQuestion(){

    }

    public void writeChat(String chatid)
    {
        Log.d("chat",chatid);
        chatsTable.child(chatid).push().setValue(null);
    }*/



    public String getCurrentUserId(){
       return authRepository.getCurrentUserUid();
    }
    public void setDownloadProfilePicListener(StorageRepository.StorageDownloadProfilePicListener downloadListener){
        storageRepository.setDownloadListener(downloadListener);
    }
    public void setDownloadMainPicListener(StorageRepository.StorageDownloadMainPicListener downloadMainPicListener){
        storageRepository.setDownloadMainPicListener(downloadMainPicListener);
    }
    public void setUploadListener(StorageRepository.StorageUploadPicListener uploadListener){
        storageRepository.setUploadListener(uploadListener);
    }
    public void writePictureToStorage(Bitmap bitmap){
        storageRepository.writePictureToStorage(bitmap,authRepository.getCurrentUserUid());
    }
    public void readMyProfilePictureFromStorage(){
        storageRepository.readPictureFromStorage(authRepository.getCurrentUserUid());
    }
    public void readPictureFromStorage(String uid){
        storageRepository.readPictureFromStorage(uid);
    }

    public boolean checkIfAuth() {
        return authRepository.checkIfAuth();
    }

    public String getCurrenUserEmail() {
        return authRepository.getCurrentUserEmail();
    }

    public Query readAllMessages(String chatId) {
        return chatsTable.child(chatId).child("Messages");
    }
    public void writeMessage(String chatId, final Message message){
        chatsTable.child(chatId).child("chat").setValue(new Chat(chatId,message.getRecipientUid(),message.getSenderUid(),message));
        chatsTable.child(chatId).child("Messages").push().setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if(messageListener!=null)
                        messageListener.onMessageSentSuccess(message);
            }
        });
    }
    private String calculateChatUid(String myUid,String otherUid){
        if(myUid.compareTo(otherUid)>0){
            return myUid+otherUid;
        }
        else {
            return otherUid + myUid;
        }
    }
    public void readChats(final Profile profile) {
        final Set<String> chatIds=new HashSet<>();
        for(Match match:profile.getMatches()){
            chatIds.add(match.getId());
        }
        chatsTable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Chat> chats=new ArrayList<>();

               for(DataSnapshot snapshot:dataSnapshot.getChildren()){
                   ChatAndMessages chatAndMessages=snapshot.getValue(ChatAndMessages.class);
                   if(chatIds.contains(chatAndMessages.getChat().getId()))
                        chats.add(chatAndMessages.getChat());
               }
               Log.d("chat_size",chats.size()+"");
                if(chatListener!=null){
                    chatListener.onChatDataChanged(chats);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void writeChat(Chat chat) {
        chat.setLastMessage(new Message(chat.getFirstUid(),"",chat.getSecondUid()));
        chatsTable.child(chat.getId()).child("chat").setValue(chat);
    }

    public void readProfilesForGuest() {
        profilesTable.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){
                    final int MAX_PROFILES_TO_SHOW=4;
                    int i=0;
                    List<Profile> profiles=new ArrayList<>();
                    for(DataSnapshot currSnapshot:snapshot.getChildren()){
                        if(i==MAX_PROFILES_TO_SHOW)
                            break;
                        profiles.add(currSnapshot.getValue(Profile.class));
                        i++;
                    }
                    if(profilesForGuestListener!=null)
                        profilesForGuestListener.onGuestProfilesChangedSuccess(profiles);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public interface  ProfilesForGuestListener{
        void onGuestProfilesChangedSuccess(List<Profile> guestProfiles);
    }
    public void setProfileGuestListener(ProfilesForGuestListener profileGuestListener){
        this.profilesForGuestListener=profileGuestListener;
    }


    public interface MessageListener{
        void onMessageSentSuccess(Message message);
    }


    public interface ProfileListener {
        void onProfileDataChangeSuccess(Profile profile);
        void onProfileDataChangeFail(String error);
    }
    public void setProfileListener(ProfileListener profileListener) {
        this.profileListener = profileListener;
    }
    public interface ProfilesListener{
        void onProfilesDataChangeSuccess(List<Profile> profiles);
        void onProfilesDataChangeFail(String error);
    }

    public void setProfilesListener(ProfilesListener profilesListener) {
        this.profilesListener = profilesListener;
    }
    public void setMatchesListener(MatchesListener matchesListener){
        this.matchesListener=matchesListener;
    }

    public interface  MatchesListener{
        void onMatchesDataChangeSuccess(List<Profile> matches);
        void onMatchesDataChangeFail(String error);
    }

    public interface QuestionsListener{
        void onQuestionsDataChangeSuccess(List<Question> questions);
        void onQuestionsDataChangeFail(String error);
    }
    public interface ChatListener{
        void onChatDataChanged(List<Chat> chats);
    }
    public void setQuestionsListener(QuestionsListener questionsListener) {
        this.questionsListener = questionsListener;
    }
    public void logout(){
        authRepository.logoutUser();
    }

    public void readQuestions(){

        questionsTable.child("english").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    List<Question> questions=new ArrayList<>();
                    for(DataSnapshot currSnapshot:snapshot.getChildren()){
                        Question question=currSnapshot.getValue(Question.class);
                        questions.add(question);
                    }
                    if(questionsListener!=null)
                         questionsListener.onQuestionsDataChangeSuccess(questions);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {



            }
        });
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public interface ReadOtherProfileListener{
        void onOtherProfileChange(Profile profile);
    }
    public void setOtherProfileListener(ReadOtherProfileListener readOtherProfileListener){
        this.readOtherProfileListener=readOtherProfileListener;
    }
    public void readOtherProfile(String uid) {

        profilesTable.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Profile profile = snapshot.getValue(Profile.class);
                    if (profileListener != null) {
                        readOtherProfileListener.onOtherProfileChange(profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (profileListener != null)
                    profileListener.onProfileDataChangeFail(error.getMessage());
            }
        });
    }
    public void uploadAndDownload(Bitmap bitmap,boolean isProfilePic){
        storageRepository.uploadAndDownload(bitmap,isProfilePic);

    }
    public void setChatListener(ChatListener chatListener){
        this.chatListener=chatListener;
    }
}
