package com.iotalabs.geoar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.example.lotalabsappui.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ListFragment extends Fragment {

    private ListView fList;
    private MyAdapter myAdapter;
    public static ArrayList<FriendData> fData = new ArrayList<>();

    private DbOpenHelper mDbOpenHelper;
    private Cursor mCursor;
    private FriendData friendData;
    private TextView no_f;
    private FrameLayout fr_list;
    private GetFriendData getTask;
    private static String IP_ADDRESS;
    private DeleteFriendData task;
    private Activity activity;
    private Handler handler = new Handler();
    private Handler handler2 = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        IP_ADDRESS=Constants.IP_ADDRESS.toString();
        fr_list = (FrameLayout) view.findViewById(R.id.frame_list);
        fr_list.setVisibility(View.VISIBLE);
        no_f = (TextView) view.findViewById(R.id.no_friend);
        mDbOpenHelper = new DbOpenHelper(getActivity());

        fList=(SwipeMenuListView)view.findViewById(R.id.friendList);
        myAdapter=new MyAdapter(getContext(),fData);
        fList.setAdapter(myAdapter);

        doWhileCursorToArray();

        // ????????? ???????????? Adapter ??????
        // fragment????????? 'this' ????????? ???????????????, Activity??? ?????? ????????? ????????? getActivity()?????? ??????

        // ????????? ?????? ????????? ??????
        SwipeMenuListView listview;
        listview = (SwipeMenuListView) view.findViewById(R.id.friendList);
        listview.setAdapter(myAdapter);
        listview.setMenuCreator(creator);
        listview.setOnSwipeListener(new SwipeMenuListView.OnSwipeListener() {
            @Override
            public void onSwipeStart(int position) {
                // swipe start
                listview.smoothOpenMenu(position);
            }

            @Override
            public void onSwipeEnd(int position) {
                // swipe end
                listview.smoothOpenMenu(position);
            }
        });

        fList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
        listview.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                task = new DeleteFriendData(getActivity());
                task.execute("http://" + IP_ADDRESS + "/deleteFriend.php",CreateQR.GetDeviceUUID(getContext()),fData.get(position).UUID,String.valueOf(fData.get(position)._id));
                getTask= new GetFriendData(getContext());//?????? ???????????? ??????
                getTask.execute( "http://" + IP_ADDRESS + "/getMyFriend.php",CreateQR.GetDeviceUUID(getContext()));
                //Toast.makeText(getActivity().getApplication(), "????????????!", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        //////////

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        doWhileCursorToArray();
    }


    public int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density; return Math.round((float) dp * density);
    }

    SwipeMenuCreator creator = new SwipeMenuCreator() {

        @Override// list ????????? ?????? ?????????
        public void create(SwipeMenu menu) {
            // create "?????????" item
            SwipeMenuItem openItem = new SwipeMenuItem(
                    getActivity());

            // create "delete" item
            SwipeMenuItem deleteItem = new SwipeMenuItem(
                    getActivity());
            // set item background
            deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                    0x3F, 0x25)));
            // set item width
            deleteItem.setWidth(dpToPx(90));
            // set a icon
            deleteItem.setIcon(R.drawable.ic_baseline_delete_forever_24);
            // add to menu
            menu.addMenuItem(deleteItem);
        }
    };
    @SuppressLint("Range")
    private void doWhileCursorToArray(){
        fData.clear();
        mDbOpenHelper.open();
        mCursor = null;
        mCursor = mDbOpenHelper.getAllColumns();
        while (mCursor.moveToNext()) {
            friendData = new FriendData(
                    mCursor.getInt(mCursor.getColumnIndex("_id")),
                    mCursor.getString(mCursor.getColumnIndex("UUID")),
                    mCursor.getString(mCursor.getColumnIndex("name"))
            );
            fData.add(friendData);
        }
        mCursor.close();
        if(fData.isEmpty()){
            no_f.setVisibility(View.VISIBLE);
        }
        else {
            no_f.setVisibility(View.INVISIBLE);
        }
        mDbOpenHelper.close();
        myAdapter.notifyDataSetChanged();
    }

    public class DeleteFriendData  extends AsyncTask<String, Void, String> {
        private Activity activity;
        private ProgressDialog progressDialog;
        private DbOpenHelper mDbOpenHelper;

        public  DeleteFriendData(Activity activity){
            this.activity=activity;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(activity, "Please Wait", null, true, true);
            progressDialog.setCanceledOnTouchOutside(false);//????????????X
            progressDialog.setCancelable(false);//????????????X
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
        }

        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(String... params) {
            String result;
            String TO_FRIEND = (String)params[1];
            String FROM_FRIEND = (String)params[2];

            String serverURL = (String)params[0];
            int position = Integer.parseInt(params[3]);
            String postParameters = "TO_FRIEND=" + TO_FRIEND + "&FROM_FRIEND=" + FROM_FRIEND ;
//??????????????? &??? ????????????

            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }


                bufferedReader.close();
                result = sb.toString();
                if(result.equals("?????? ??????!")){//????????? ????????? ??????????????? ?????? ????????? ?????? ?????? ????????????????????? ??????
                    mDbOpenHelper = new DbOpenHelper(activity);
                    mDbOpenHelper.open();
                    mDbOpenHelper.deleteColumn(position);
                    mDbOpenHelper.close();
                    toast("?????? ??????!");
                }
                else{
                    toast("??????????????? ??????????????????.");
                }

                backDoWhileCursorToArray();
                return sb.toString();

            } catch (Exception e) {
                toast("??????????????? ??????????????????.");
                return new String("Error: " + e.getMessage());
            }

        }
    }
    public void backDoWhileCursorToArray(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                doWhileCursorToArray();
            }
        });
    }
    public void toast(String msg){
        handler2.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(),msg , Toast.LENGTH_SHORT).show();
            }
        });
    }
}