package com.example.android.tcpclient;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import fr.arnaudguyon.smartgl.math.Vector3D;
import fr.arnaudguyon.smartgl.opengl.LightParallel;
import fr.arnaudguyon.smartgl.opengl.Object3D;
import fr.arnaudguyon.smartgl.opengl.RenderPassObject3D;
import fr.arnaudguyon.smartgl.opengl.SmartColor;
import fr.arnaudguyon.smartgl.opengl.SmartGLRenderer;
import fr.arnaudguyon.smartgl.opengl.SmartGLView;
import fr.arnaudguyon.smartgl.opengl.SmartGLViewController;
import fr.arnaudguyon.smartgl.opengl.Texture;
import fr.arnaudguyon.smartgl.tools.WavefrontModel;
import fr.arnaudguyon.smartgl.touch.TouchHelperEvent;

import static com.example.android.tcpclient.R.id.smartGLView;

public class ClientActivity extends Activity implements SmartGLViewController{

    private ListView mList;
    private ArrayList<String> arrayList;
    private ClientListAdapter mAdapter;
    private TcpClient mTcpClient;

    private LineGraphSeries<DataPoint> series;
    private char identifier = '?'; // TODO this is useless, delete as soon as possiible

    private SmartGLView mSmartGLView;

    private Object3D mObject3D;

    private Texture mObjectTexture;

    private RenderPassObject3D mRenderPassObject3D;
    private RenderPassObject3D mRenderPassObject3DColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //TODO : This

        mSmartGLView = (SmartGLView) findViewById(smartGLView);
        mSmartGLView.setDefaultRenderer(this);
        mSmartGLView.setController(this);


        arrayList = new ArrayList<String>();

        final EditText editText = (EditText) findViewById(R.id.editText);
        Button send = (Button) findViewById(R.id.send_button);

        //Relate the listView from java to the one created in xml
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new ClientListAdapter(this, arrayList);
        mList.setAdapter(mAdapter);

        //Create Graph and Instantiate

        GraphView graph = (GraphView) findViewById(R.id.graph);

        // activate horizontal zooming and scrolling
        graph.getViewport().setScalable(true);

// activate horizontal scrolling
        graph.getViewport().setScrollable(true);

// activate horizontal and vertical zooming and scrolling
        graph.getViewport().setScalableY(true);

// activate vertical scrolling
        graph.getViewport().setScrollableY(true);

        series = new LineGraphSeries<>();
        graph.addSeries(series);

        // Set onClick Listener

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = editText.getText().toString();

                //add the text in the arrayList
                arrayList.add("C: " + message);

                //sends the message to the server
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(message);
                }

                //refresh the list
                mAdapter.notifyDataSetChanged();
                editText.setText("");
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        // disconnect
        mTcpClient.stopClient();
        mTcpClient = null;

        // Pause 3D View
        if (mSmartGLView != null) {
            mSmartGLView.onPause();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSmartGLView != null) {
            mSmartGLView.onResume();
        }
    }

    @Override
    public void onPrepareView(SmartGLView smartGLView) {

        Context context = smartGLView.getContext();

        // Add RenderPass for Sprites & Object3D
        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
        mRenderPassObject3D = new RenderPassObject3D(RenderPassObject3D.ShaderType.SHADER_TEXTURE_LIGHTS, true, true);
        mRenderPassObject3DColor = new RenderPassObject3D(RenderPassObject3D.ShaderType.SHADER_COLOR, true, false);
        renderer.addRenderPass(mRenderPassObject3D);
        renderer.addRenderPass(mRenderPassObject3DColor);

        renderer.setDoubleSided(false);

        SmartColor lightColor = new SmartColor(1, 1, 1);
        Vector3D lightDirection = new Vector3D(0, 1, 1);
        lightDirection.normalize();
        LightParallel lightParallel = new LightParallel(lightColor, lightDirection);
        renderer.setLightParallel(lightParallel);

        mObjectTexture = new Texture(context, R.drawable.coloredbg);

        putColoredCube(context);
    }

    private void dropAllObject3D() {
        mRenderPassObject3D.clearObjects();
        mRenderPassObject3DColor.clearObjects();
    }

    void putColoredCube(Context context) {
        dropAllObject3D();
        WavefrontModel modelColored = new WavefrontModel.Builder(context, R.raw.cube_color_obj)
                .create();
        mObject3D = modelColored.toObject3D();
        mObject3D.setPos(0, 0, -4);
        mRenderPassObject3DColor.addObject(mObject3D);
    }

    @Override
    public void onReleaseView(SmartGLView smartGLView) {
        if (mObjectTexture != null) {
            mObjectTexture.release();
        }
    }

    @Override
    public void onResizeView(SmartGLView smartGLView) {
        onReleaseView(smartGLView);
        onPrepareView(smartGLView);
    }

    @Override
    public void onTick(SmartGLView smartGLView) {

    }

    @Override
    public void onTouchEvent(SmartGLView smartGLView, TouchHelperEvent event) {
        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
        float frameDuration = renderer.getFrameDuration();

        // add some rotation movement
        if (mObject3D != null) {
            float rx = mObject3D.getRotX() + 50 * frameDuration;
            float ry = mObject3D.getRotY() + 37 * frameDuration;
            float rz = mObject3D.getRotZ() + 26 * frameDuration;
            mObject3D.setRotation(rx, ry, rz);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mTcpClient != null) {
            // if the client is connected, enable the connect button and disable the disconnect one
            menu.getItem(1).setEnabled(true);
            menu.getItem(0).setEnabled(false);
        } else {
            // if the client is disconnected, enable the disconnect button and disable the connect one
            menu.getItem(1).setEnabled(false);
            menu.getItem(0).setEnabled(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.connect:

                // Get the IP Address input
                final EditText ipText = (EditText) findViewById(R.id.ip);
                String ipAddress = ipText.getText().toString();

                // Connect to the server

                new ConnectTask(ipAddress).execute("");
                return true;
            case R.id.disconnect:
                // Disconnect
                mTcpClient.stopClient();
                mTcpClient = null;
                // Clear the data set
                arrayList.clear();
                // Notify the adapter that the data set has changed.
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        private String IP_ADDRESS;

        public ConnectTask(String ipAdress){
            super();
            IP_ADDRESS = ipAdress;
        }

        private void update3D(SmartGLView smartGLView, int[] rot, int[] trans){
            SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
            float frameDuration = renderer.getFrameDuration();

            // add some rotation movement
            if (mObject3D != null) {
                float rx = mObject3D.getRotX() + rot[0] * frameDuration;
                float ry = mObject3D.getRotY() + rot[1] * frameDuration;
                float rz = mObject3D.getRotZ() + rot[2] * frameDuration;
                mObject3D.setRotation(rx, ry, rz);
            }

            if (mObject3D != null) {
                float x = mObject3D.getPosX() + trans[0] * frameDuration;
                float y = mObject3D.getPosY() + trans[1] * frameDuration;
                float z = mObject3D.getPosZ() + trans[2] * frameDuration;
                mObject3D.setPos(x, y, z);
            }
        }

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TcpClient(IP_ADDRESS, new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //This method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //TODO When information is recieved, it is treated here, so for the graph : put instruction here

            if(values[0].charAt(0) == identifier){
                String[] data = values[0].substring(2).split(",");
                double x = Double.parseDouble(data[0]);
                double y = Double.parseDouble(data[1]);
                series.appendData(new DataPoint(x,y), true, 200);

                update3D(mSmartGLView, new int[] {250,250,250}, new int[] {1,1,1});
            }

            //in the arrayList we add the messaged received from server
            arrayList.add(values[0]);
            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            mAdapter.notifyDataSetChanged();
        }
    }
}