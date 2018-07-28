package edu.buffalo.cse.cse486586.simpledynamo;


//References for syntaxes : Oracle docs, Android Developers
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    public String c_port = "";
    public String c_hash = "";
    public String c_succ = "";
    public String c_pred = "";
    public String c_succ1 = "";
    public String c_pred1 = "";
    public String c_pred_hash = "";
    public String c_succ_hash = "";
    public String finalstr;
    public String insert_port;
    public HashSet<String> set = new HashSet<String>();
//    public HashSet<String> keyset = new HashSet<String>();
//    public HashSet<String> keyset1 = new HashSet<String>();
    public int flag = 0;
    public String myPort;
    public long ml;
    static final String[] ports = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    static final int SERVER_PORT = 10000;
    static final String[] portid ={"5554","5556","5558","5560","5562"};
    public static HashMap<String,String> avd_id= new HashMap<String, String>();
    public static HashMap<String,String> hash_check = new HashMap<String,String>();
    public static HashMap<String,ArrayList<String>> value_time = new HashMap<String,ArrayList<String>>();
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";
	public BlockingQueue<String> squeue;
	public BlockingQueue<String> insertqueue1;
	public BlockingQueue<String> squeue1;//for @query
    public StringBuilder sbuf = new StringBuilder();
    public StringBuilder sbuf1 = new StringBuilder();
    public StringBuilder sqbuf = new StringBuilder();//@query
    public StringBuilder query1 = new StringBuilder();//own string
    public StringBuilder query2 = new StringBuilder();//replica string from succ1
    public StringBuilder query3 = new StringBuilder();//replica string from succ2
    public static HashMap<String,String> port_succ2 = new HashMap<String,String>();
    public static HashMap<String,String> port_succ1 = new HashMap<String,String>();
//    public static HashMap<String, ArrayList<String>> key_value = new HashMap<String, ArrayList<String>>();
//    public static HashMap<String, ArrayList<String>> key_value1 = new HashMap<String, ArrayList<String>>();

    public boolean thread_flag = false;
    ArrayList<String> rest = new ArrayList<String>();
    ArrayList<String> failure_list = new ArrayList<String>();
	ArrayList<Node> nodes = new ArrayList<Node>();
    String[] hashes = {"177ccecaec32c54b82d5aaafc18a2dadb753e3b1","208f7f72b198dadd244e61801abe1ec3a4857bc9","33d6357cfaaf0f72991b0ecd8c56da066613c089","abf0fd8db03e5ecb199a9b82929e9db79b909643","c25ddd596aa7c81fa12378fa725f706d54325d12"};

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
        String k = selection;
        String h = "";
        File dir = getContext().getFilesDir();
            try {
                    h = genHash(selection);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

        if(k.equals("@") ) {
            if (dir.exists()) {
                File[] files = dir.listFiles();
                Log.d("@Delete@", "Total number of files to be deleted: " + files.length);
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    String strFileName = file.getName();
                    Log.d("@Delete@", "File : " + strFileName);
                    getContext().deleteFile(strFileName);
                    Log.d("@Delete@", "Success");
                }
            }
        }
        else if(k.equals("*"))
        {
            if (dir.exists()) {
                File[] files = dir.listFiles();
                Log.d("@Delete@","Total number of files to be deleted: "+files.length);
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    String strFileName = file.getName();
                    Log.d("@Delete@","File : "+strFileName);
                    getContext().deleteFile(strFileName);
                    Log.d("@Delete@","Success");
                }
            }

            //call clienttask of c_port
            String imsg = String.format("*del*:%s",c_port);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
        }
        //del specific key and its replicas from its successors
        else {
            ArrayList needed_ports = return_port(h);
            if (needed_ports.contains(c_port)) {
                if (file_exists(selection)) {
                    getContext().deleteFile(selection);
                }
                String imsg = String.format("del_specific:%s",selection);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
            }
            else{
                String imsg = String.format("del_specific:%s", selection);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
            }
        }


		return 0;
	}

	public void deleteall(){
        File dir = getContext().getFilesDir();
	    File[] files = dir.listFiles();
                Log.d("@Delete@", "Total number of files to be deleted: " + files.length + "c_port" +c_port);
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    String strFileName = file.getName();
                    Log.d("@Delete@", "File : " + strFileName);
                    getContext().deleteFile(strFileName);
                    Log.d("@Delete@", "Success");
                }
    }
	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

        String hash_key = "";
        String filename = values.getAsString("key");
        String string = values.getAsString("value");
        Log.d("string to be stored",string);
        try {
             hash_key = genHash(filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d("Key ",filename);
        Log.d("Hash to be routed",hash_key);
        Log.d("insert","Node Size :"+nodes.size());
        ArrayList needed_ports = return_port(hash_key);
        reset();
        int insert_counter = 0;
        if(needed_ports.contains(c_port))
        {
            Log.d("Insert","Belongs to one of the three nodes!Replicate only on two others!");
            try {
                ml = System.currentTimeMillis();
                String string_curr = string + "!" + String.valueOf(ml);
                write_file(filename,string_curr);
                insert_counter++;
                Log.v("insert", values.toString());
                String imsg = String.format("replicate:%s:%s:%s", filename, string, String.valueOf(insert_counter));
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
//                insertqueue1 = new ArrayBlockingQueue<String>(1);
//                String ack = insertqueue1.take();
            } catch (Exception e) {
                Log.e("Insert", "File write failed at node" + c_port);
            }
        }
        else{
            try {
                Log.d("Insert", "Doesnt Belong to current node!Replicate into 3 nodes!");
                String imsg = String.format("replicate:%s:%s:%s", filename, string, String.valueOf(insert_counter));
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
//                insertqueue1 = new ArrayBlockingQueue<String>(1);
//                String ack = insertqueue1.take();
            }catch (Exception e){}
        }

        return uri;
	}

	public void reset(){
	    flag = 0;
    }



    public String find_recentval(String val){
        Log.d("find_recentval",val);
	    long final_val = 0;
	    long final_val1 = 0;
	    String final_string = "";
	    String final_string1 = "";


	    String[] v = val.split("\\$");
	    int vsize = v.length;
	    if(vsize == 1){
	        String[] v1 = v[0].split("\\!");
	        final_string = v1[0];
	        Log.d("STring",final_string);
            final_val = Long.parseLong(v1[1]);
            Log.d("Val",String.valueOf(final_val));
	    }
        if(vsize == 2){
	    String[] v1 = v[0].split("\\!");
        String[] v2 = v[1].split("\\!");

        if(Long.parseLong(v1[1]) >= Long.parseLong(v2[1])){
        final_val = Long.parseLong(v1[1]);
        final_string = v1[0];
        }
        else{
        final_val = Long.parseLong(v2[1]);
        final_string = v2[0];
        }

        }

        if(vsize == 3){

            String[] v1 = v[0].split("\\!");
            String[] v2 = v[1].split("\\!");
            String[] v3 = v[2].split("\\!");

            if(Long.parseLong(v1[1]) >= Long.parseLong(v2[1])){
            final_val1 = Long.parseLong(v1[1]);
            final_string1 = v1[0];
            }
            else{
            final_val1 = Long.parseLong(v2[1]);
            final_string1 = v2[0];
            }

            if(final_val1 >= Long.parseLong(v3[1])){
            final_val = final_val1;
            final_string = final_string1;
            }
            else{
            final_val = Long.parseLong(v3[1]);
            final_string = v3[0];
            }

        }
	    return final_string;
    }

        public String find_recentval1(String val){

	    long final_val = 0;
	    long final_val1 = 0;
	    String final_string = "";
	    String final_string1 = "";


	    String[] v = val.split("\\$");
	    int vsize = v.length;
	    if(vsize == 1){
	        String[] v1 = v[0].split("\\!");
	        final_string = v1[0];
	        final_val = Long.parseLong(v1[1]);
        }
        if(vsize == 2){
	    String[] v1 = v[0].split("\\!");
        String[] v2 = v[1].split("\\!");

        if(Long.parseLong(v1[1]) >= Long.parseLong(v2[1])){
        final_val = Long.parseLong(v1[1]);
        final_string = v1[0];
        }
        else{
        final_val = Long.parseLong(v2[1]);
        final_string = v2[0];
        }

        }

        if(vsize == 3){

            String[] v1 = v[0].split("\\!");
            String[] v2 = v[1].split("\\!");
            String[] v3 = v[2].split("\\!");

            if(Long.parseLong(v1[1]) >= Long.parseLong(v2[1])){
            final_val1 = Long.parseLong(v1[1]);
            final_string1 = v1[0];
            }
            else{
            final_val1 = Long.parseLong(v2[1]);
            final_string1 = v2[0];
            }

            if(final_val1 >= Long.parseLong(v3[1])){
            final_val = final_val1;
            final_string = final_string1;
            }
            else{
            final_val = Long.parseLong(v3[1]);
            final_string = v3[0];
            }

        }
        String return_string = final_string + "!" +String.valueOf(final_val);
	    return return_string;
    }
    public ArrayList<String> return_port(String hash_key){
	    ArrayList<String> needed_ports = new ArrayList<String>();
	    String coordinator = null;
	    for(int i = 1; i < hashes.length;i++) {
        if (hashes[i - 1].compareTo(hash_key) < 0 && (hashes[i].compareTo(hash_key) > 0))
        {
            coordinator = hash_check.get(hashes[i]);
            needed_ports.add(coordinator);
            flag = 1;
            break;
        }
    }
    if (flag == 0) {
        coordinator = hash_check.get("177ccecaec32c54b82d5aaafc18a2dadb753e3b1");
	    needed_ports.add(coordinator);
    }

        needed_ports.add(port_succ1.get(coordinator));//add succ1
	    needed_ports.add(port_succ2.get(coordinator));//add succ2


	return needed_ports;
    }


    public String return_coordinator(String hash_key){
        String coordinator = null;
	    for(int i = 1; i < hashes.length;i++) {
        if (hashes[i - 1].compareTo(hash_key) < 0 && (hashes[i].compareTo(hash_key) > 0))
        {
            coordinator = hash_check.get(hashes[i]);
            flag = 1;
            break;
        }
    }
    if (flag == 0) {
        coordinator = hash_check.get("177ccecaec32c54b82d5aaafc18a2dadb753e3b1");
    }
    return  coordinator;
    }

    public void write_file(String filename,String string){

        try {
            FileOutputStream outputStream;
            Log.d("insert", "Write Starts" + string);
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            Log.d("insert", "write successful");
            Log.d("Finalwrite", "Stored at node :" + c_port + "Message stored:" + string);
            outputStream.close();
        }catch (IOException e){

        }
    }

    public boolean file_exists(String filename) {
    String pof = getContext().getFilesDir().getAbsolutePath() + "/" + filename;
    File file_obj = new File(pof);
    return file_obj.exists();
}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        Log.d("Final","CurrentPort: "+myPort);

        //Initialization
        avd_id.put("5554", "11108");
        avd_id.put("5556", "11112");
        avd_id.put("5558", "11116");
        avd_id.put("5560", "11120");
        avd_id.put("5562", "11124");

        hash_check.put("33d6357cfaaf0f72991b0ecd8c56da066613c089","5554");
        hash_check.put("208f7f72b198dadd244e61801abe1ec3a4857bc9","5556");
        hash_check.put("abf0fd8db03e5ecb199a9b82929e9db79b909643","5558");
        hash_check.put("c25ddd596aa7c81fa12378fa725f706d54325d12","5560");
        hash_check.put("177ccecaec32c54b82d5aaafc18a2dadb753e3b1","5562");

        port_succ2.put("5562","5554");
        port_succ2.put("5560","5556");
        port_succ2.put("5558","5562");
        port_succ2.put("5556","5558");
        port_succ2.put("5554","5560");

        port_succ1.put("5562","5556");
        port_succ1.put("5560","5562");
        port_succ1.put("5558","5560");
        port_succ1.put("5556","5554");
        port_succ1.put("5554","5558");


        //for later uses
        c_port = portStr;

        try {
            c_hash = genHash(c_port);
        }catch (NoSuchAlgorithmException e)
        {
            Log.d(TAG, "Hash Value not generated!");
        }

        //adding nodes to a list
        for (int i = 0; i < portid.length; i++){
            try{
                String temp_hash = genHash(portid[i]);
                Node new_node = new Node(portid[i],temp_hash);
                if (!(set.contains(portid[i]))) {
                    set.add(portid[i]);
                    nodes.add(new_node);
                    Log.d("Server", "node added to dynamo!");
                }
            }catch (Exception e){}
        }
        Log.d("Server", "Hash set Size:" + set.size());
        Collections.sort(nodes);
        Log.d("Server", "Node size :" + nodes.size());
        assign_pred_succ();
        //Adding rest to an arraylist
        for(int i = 0;i < portid.length; i++ ){
            if(!portid[i].equals(c_port)){
                rest.add(portid[i]);
            }
        }
        Log.d("rest_size","is:"+rest.size());
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }catch (IOException e){
            Log.e(TAG, "Can't create a ServerSocket");
        }

        String imsg = "failure:"+"useless";
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);

        return false;
	}

    private void find_failurelist() {
        failure_list.add(c_pred);
        failure_list.add(c_pred1);
        failure_list.add(c_port);
    }

    //assign_pred_succ of 5 ports
    public void assign_pred_succ(){
    try {
        if (nodes.size() == 5) {
            Log.d("node size :", String.valueOf(nodes.size()));
            int n = nodes.size();
            for (int i = 0; i < n; i++) {
                if (i == 0) {
                    nodes.get(i).setSuccesor(nodes.get(i + 1));
                    nodes.get(i).setSuccesor1(nodes.get(i + 2));
                    nodes.get(i).setPredecessor(nodes.get(n - 1));
                    nodes.get(i).setPredecessor1(nodes.get(n - 2));
                }
                if (i == n - 1) {
                    nodes.get(i).setSuccesor(nodes.get(0));
                    nodes.get(i).setSuccesor1(nodes.get(1));
                    nodes.get(i).setPredecessor(nodes.get(i - 1));
                    nodes.get(i).setPredecessor1(nodes.get(i - 2));
                }
                if (i == 1) {
                    nodes.get(i).setSuccesor(nodes.get(i + 1));
                    nodes.get(i).setSuccesor1(nodes.get(i + 2));
                    nodes.get(i).setPredecessor(nodes.get(i - 1));
                    nodes.get(i).setPredecessor1(nodes.get(n - 1));
                }
                if (i == n - 2) {
                    nodes.get(i).setSuccesor(nodes.get(i + 1));
                    nodes.get(i).setSuccesor1(nodes.get(0));
                    nodes.get(i).setPredecessor(nodes.get(i - 1));
                    nodes.get(i).setPredecessor1(nodes.get(i - 2));
                }
                if (i == n - 3) {
                    nodes.get(i).setSuccesor(nodes.get(i + 1));
                    nodes.get(i).setSuccesor1(nodes.get(i + 2));
                    nodes.get(i).setPredecessor(nodes.get(i - 1));
                    nodes.get(i).setPredecessor1(nodes.get(i - 2));
                }
                if (nodes.get(i).port_num.equals(c_port)) {
                    c_pred = nodes.get(i).predecessor.port_num;
                    c_pred1 = nodes.get(i).predecessor1.port_num;
                    c_succ = nodes.get(i).succesor.port_num;
                    c_succ1 = nodes.get(i).succesor1.port_num;
                    c_pred_hash = genHash(c_pred);
                    c_succ_hash = genHash(c_succ);
                }
            }
            Log.d("Closure:", ":" + c_port + ":" + c_hash + ":" + c_succ + ":" + c_pred);
            Log.d("Closure: Node Size:", ":" + nodes.size());
            for (int j = 0; j < nodes.size(); j++) {
                display_nodes(nodes.get(j));
            }

        }
    }catch (Exception e){}
}

	//ServerTask
	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        public Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
         @Override
         protected Void doInBackground(ServerSocket... sockets) {

             while (true) {
                 try {
                     String ack = "";
                     Log.d("Server", "Server is Listening");
                     ServerSocket serverSocket = sockets[0];
                     Socket sock = serverSocket.accept();
                     StringBuilder stringBuilder = new StringBuilder();
                     ContentResolver mContentResolver;
                     String fstr = "";
                     mContentResolver = getContext().getContentResolver();
                     Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
                     DataOutputStream dout;
                     DataInputStream din = new DataInputStream(sock.getInputStream());
                     String message = din.readUTF();
                     Log.d("Server","message recieved:"+message);

                     String[] m = message.split("\\:");
//                     Log.d(TAG,"Split Messages"+m[0]+" "+m[1]);

                     if(m[0].equals("insert_replicas")){

                        Log.d("Server","inside insert_Replicas");
                        dout = new DataOutputStream(sock.getOutputStream());
                        ack="ack";
                        ack.trim();
                        dout.writeUTF(ack);
                        dout.flush();

                        Log.d(m[1], m[2]);
                        ml = System.currentTimeMillis();
                        String val = m[2] + "!" + String.valueOf(ml);
                        write_file(m[1],val);

                     }

                    if(m[0].equals("query_replicas")){

                         Log.d("Server", "inside query_replicas");
                         String q_key = m[1];
                         if(file_exists(q_key)){
                             String val = queryforresult(q_key);
                             ack = "result" + "$" + q_key + "$" + val;
                             Log.d("ack",ack);
                             ack.trim();
                             dout = new DataOutputStream(sock.getOutputStream());
                             dout.writeUTF(ack);
                             dout.flush();
                         }
                         else{
                             ack = "result" + "$" + "nullcursor";
                             Log.d("ack",ack);
                             ack.trim();
                             dout = new DataOutputStream(sock.getOutputStream());
                             dout.writeUTF(ack);
                             dout.flush();
                         }
                     }

                     if(m[0].equals("@query")){
                         StringBuilder query = new StringBuilder();
                         Log.d("Server", "inside @query");
                         String[] check_port = new String[]{"uselesscrap"};
                         Cursor resultCursor =  mContentResolver.query(mUri, null,"@" , check_port, null);
                         for (resultCursor.moveToFirst(); !resultCursor.isAfterLast(); resultCursor.moveToNext()) {
                            int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
                            int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
                            String returnKey = resultCursor.getString(keyIndex);
                            String returnValue = resultCursor.getString(valueIndex);
                            String add_str = returnKey + "*" + returnValue + "|";
                            Log.d("add_Str",add_str);
                            query.append(add_str);
                         }

                         resultCursor.close();
                         ack = "string" + "$" + query.toString();
                        dout = new DataOutputStream(sock.getOutputStream());
                        ack.trim();
                        dout.writeUTF(ack);
                        dout.flush();
                     }


                    if(m[0].equals("query*")){
                        Log.d("Server","inside query*");
                        String[] check_port = new String[]{"uselesscrap"};
                        Cursor resultCursor = mContentResolver.query(mUri, null, "@",check_port, null);
                        for (resultCursor.moveToFirst(); !resultCursor.isAfterLast(); resultCursor.moveToNext()) {
                            int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
                            int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
                            String returnKey = resultCursor.getString(keyIndex);
                            String returnValue = resultCursor.getString(valueIndex);
                            String add_str = returnKey + "*" + returnValue + "|";
                            sbuf1.append(add_str);
                        }
                        resultCursor.close();
                        ack = "string" + "$" + sbuf1.toString();
                        ack.trim();
                        dout = new DataOutputStream(sock.getOutputStream());
                        dout.writeUTF(ack);
                        dout.flush();
                    }
                    if(m[0].equals("*del")){
                        dout = new DataOutputStream(sock.getOutputStream());
                        ack="ack";
                        ack.trim();
                        dout.writeUTF(ack);
                        dout.flush();
                        mContentResolver.delete(mUri, "@", null);
                    }
                    if(m[0].equals("del_replicas")){
                        dout = new DataOutputStream(sock.getOutputStream());
                        ack="ack";
                        ack.trim();
                        dout.writeUTF(ack);
                        dout.flush();
                        String file = m[1];
                        if(file_exists(file)){
                            getContext().deleteFile(file);
                        }
                    }

                     Log.d("Server", "Message complete");
                     sock.close();
                 }catch (Exception e) {
                     Log.e(TAG, "No Connection");
                     e.printStackTrace();
                 }
             }
         }
		protected void onProgressUpdate(String... strings) {
         }
     }

    public void display_nodes(Node node){
        Log.d("Port Number",node.port_num);
        Log.d("Hash Value",node.hash_port);
        if(node.succesor == null)
            Log.d("Successor Node id", "null");
        else Log.d("Successor Node id", node.succesor.port_num);
        if(node.succesor1 == null)
            Log.d("Successor1 Node id", "null");
        else Log.d("Successor1 Node id", node.succesor1.port_num);
        if(node.predecessor == null)
            Log.d("Predecessor Node id", "null");
        else Log.d("Predecessor Node id", node.predecessor.port_num);
        if(node.predecessor1 == null)
            Log.d("Successor1 Node id", "null");
        else Log.d("Successor1 Node id", node.predecessor1.port_num);
     }

	//ClientTask
    private class ClientTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... msgs)
        {
            Log.d("Client", "Message recieved is:" + msgs[0]);
            String remotePort;
            String append_string = "";
            StringBuilder msg_from_ports = new StringBuilder();
            String Client_msg = String.valueOf(msgs[0]);
            String[] pstr = Client_msg.split("\\:");
            if(pstr[0].equals("failure")) {
                HashMap<String, ArrayList<String>> key_value = new HashMap<String, ArrayList<String>>();
                HashSet<String> keyset = new HashSet<String>();
                deleteall();
                for (int i = 0; i < rest.size(); i++) {
                    try {
                        remotePort = avd_id.get(rest.get(i));
                        Log.d("ack ", "remotePort" + remotePort);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        String msgToSend = "@query:" + "useless";
                        msgToSend = msgToSend.trim();
                        Log.d("Client", "@querying message :" + msgToSend);
                        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                        dout.writeUTF(msgToSend);
                        dout.flush();

                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        String ack = din.readUTF();
                        if(ack != null) {
                            Log.d("failure", ack);
                            String[] mesg = ack.split("\\$");
                            Log.d("mesg","mesglenght"+mesg.length);
                            if (mesg[0].equals("string")) {
                                if(mesg.length == 2){
                                    Log.d("mesg[1]",mesg[1]);
                                    msg_from_ports.append(mesg[1]);
                                }
                            }
                        }
                        socket.close();
                    } catch (Exception e) {
                        Log.e("Client", "ClientTask socket Exception");
                        e.printStackTrace();
                    }
                }
                Log.d("client","msg"+msg_from_ports.toString());
                    String rest_msgs = msg_from_ports.toString();
                    Log.d("rest msgs", rest_msgs);
                if (!rest_msgs.isEmpty() && rest_msgs.length() > 1)
                    {
                    String[] pairs = rest_msgs.split("\\|");
//                    Log.d("pairs length", "" + pairs.length);
//                    for (int i = 0; i < pairs.length; i++) {
//                        Log.d("=", pairs[i]);
//                    }
                    for (int i = 0; i < pairs.length; i++) {
                        Log.d("pair","Pairs: "+ pairs[i]);
                        String[] key_val = pairs[i].split("\\*");
//                        Log.d(key_val[0],key_val[1]);
                        String key = key_val[0];
                        String val = key_val[1];
                        Log.d("Failure", "key:" + key + "val:" + val);

                        if (keyset.contains(key)) {
                            ArrayList<String> new_al = new ArrayList<String>();
                            ArrayList<String> al = key_value.get(key);
                            for (int k = 0; k < al.size(); k++) {
                                new_al.add(al.get(k));
                            }
                            new_al.add(val);
                            key_value.put(key, new_al);
                        } else {
                            ArrayList<String> vals = new ArrayList();
                            vals.add(val);
                            Log.d("pairval:", key + " " + val);
                            key_value.put(key, vals);
                            keyset.add(key);
                        }
                    }
                    for (String temp : keyset) {
                        Log.d("Failure", temp);
                        ArrayList<String> al = key_value.get(temp);
                        String[] array = al.toArray(new String[al.size()]);
                        StringBuilder sb = new StringBuilder();
                        for (int l = 0; l < array.length; l++) {
                            Log.d("valuse",array[l]);
                            sb.append(array[l]);
                            sb.append("$");
                        }
                        Log.d("String ", sb.toString());
                        String final_val = find_recentval1(sb.toString());
                        Log.d("checking final val",final_val);
                        String belongsto = "";
                        try {
                            belongsto = return_coordinator(genHash(temp));
                            reset();
                        } catch (Exception e) {
                        }
                        Log.d("belongsto",belongsto);
                        find_failurelist();

                        Log.d("failure list",failure_list.get(0)+"|"+failure_list.get(1)+"|"+failure_list.get(2));

                        if (failure_list.contains(belongsto)) {
                            write_file(temp, final_val);
                            Log.d("inserted",temp+":"+final_val);
                        }
                    }
                }

                thread_flag = true;
            }
            if(pstr[0].equals("@query")){
                StringBuilder stringBuilder = new StringBuilder();
                HashSet<String> keyset1 = new HashSet<String>();
                HashMap<String, ArrayList<String>> key_value1 = new HashMap<String, ArrayList<String>>();
                for (int i = 0; i < rest.size(); i++) {
                    try {
                        remotePort = avd_id.get(rest.get(i));
                        Log.d("ack ", "remotePort" + remotePort);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        String msgToSend = "@query:" + "useless";
                        msgToSend = msgToSend.trim();
                        Log.d("Client", "@querying message :" + msgToSend);
                        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                        dout.writeUTF(msgToSend);
                        dout.flush();

                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        String ack = din.readUTF();
                        if(ack != null) {
                            String[] mesg = ack.split("\\$");
                            if(mesg.length == 2){
                            msg_from_ports.append(mesg[1]);}
                        }
                        socket.close();
                    } catch (Exception e) {
                        Log.e("Client", "ClientTask socket Exception");
                        e.printStackTrace();
                    }
                }
                Log.d("msg from ports",msg_from_ports.toString());
                    String rest_msgs = msg_from_ports.toString();
                    Log.d("@query_Client", rest_msgs);
                if (!rest_msgs.isEmpty() && rest_msgs.length() > 1)
                    {
                    String[] pairs = rest_msgs.split("\\|");
//                    Log.d("pairs length", "" + pairs.length);
//                    for (int i = 0; i < pairs.length; i++) {
//                        Log.d("=", pairs[i]);
//                    }
                    for (int i = 0; i < pairs.length; i++) {
                        Log.d("pair", pairs[i]);
                        String[] key_val = pairs[i].split("\\*");
//                        Log.d(key_val[0],key_val[1]);
                        String key = key_val[0];
                        String val = key_val[1];
                        Log.d("@query_Client", "key:" + key + "val:" + val);

                        if (keyset1.contains(key)) {
                            ArrayList<String> new_al = new ArrayList<String>();
                            ArrayList<String> al = key_value1.get(key);
                            for (int k = 0; k < al.size(); k++) {
                                new_al.add(al.get(k));
                            }
                            new_al.add(val);
                            key_value1.put(key, new_al);
                        } else {
                            ArrayList<String> vals = new ArrayList();
                            vals.add(val);
                            Log.d("pairval:", key + " " + val);
                            key_value1.put(key, vals);
                            keyset1.add(key);
                        }
                    }
                    for (String temp : keyset1) {
                        Log.d("@key", temp);
                        ArrayList<String> al = key_value1.get(temp);
                        String[] array = al.toArray(new String[al.size()]);
                        StringBuilder sb = new StringBuilder();
                        for (int l = 0; l < array.length; l++) {
                            Log.d("Qvaluse",array[l]);
                            sb.append(array[l]);
                            sb.append("$");
                        }
                        Log.d("String ", sb.toString());
                        String final_val = find_recentval1(sb.toString());
                        Log.d("checking final val",final_val);
                        String belongsto = "";
                        try {
                            belongsto = return_coordinator(genHash(temp));
                            reset();
                        } catch (Exception e) {
                        }
                        Log.d("Qbelongsto",belongsto);
                        find_failurelist();

                        Log.d("Qfailure list",failure_list.get(0)+"|"+failure_list.get(1)+"|"+failure_list.get(2));

                        if (failure_list.contains(belongsto)) {
                            append_string += temp + "*" + final_val + "|";
                             }
                    }
                    Log.d("Qappend_string",append_string);
                    try {
                        squeue1.put(append_string);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        squeue1.put("nullcursor");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (pstr[0].equals("replicate")) {

                    String file = pstr[1];
                    String val = pstr[2];
                    String insert_counter = pstr[3];
                    String file_hash = "";
                    try {
                         file_hash = genHash(file);
                        }catch (NoSuchAlgorithmException e){
                    }

                    ArrayList<String> needed_ports = return_port(file_hash);
                    reset();
                    if(Integer.parseInt(insert_counter) == 1){
                     needed_ports.remove(c_port);
                    }
                    String[] np_array = needed_ports.toArray(new String[needed_ports.size()]);
                    for(int i = 0; i < np_array.length; i++) {
                        Log.d("np_array",np_array[i]);
                    }

                    for(int i = 0; i < np_array.length; i++) {
                        try {
                            remotePort = avd_id.get(np_array[i]);
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            Log.d("remotePort",remotePort);
                            Log.d("send to avd: ", np_array[i]);
                            String msgToSend = "insert_replicas" + ":" + file + ":" + val;
                            msgToSend = msgToSend.trim();
                            Log.d("Client", "Message to be sent:" + msgToSend);
                            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                            dout.writeUTF(msgToSend);
                            dout.flush();
                            Log.d("Client","flushed");
                            DataInputStream din = new DataInputStream(socket.getInputStream());
                            String message = din.readUTF();
                            if(message != null) {
                                Log.d("CLient", message);
                                if (i < (np_array.length - 1)) {
                                    Log.d("Client", "Inside l-1 ");
                                    if (message.equals("ack")) {
                                        Log.d("Client", "Replicate Msg sent to succ " + np_array[i]);
                                    }
                                } else if (i == np_array.length - 1) {
                                    Log.d("Client", "Inside l ");
                                    if (message.equals("ack")) {
                                        Log.d("Client", "Replicate Msg sent to succ " + np_array[i]);
                                        //insertqueue1.put(message);
                                    }
                                }
                            }
                            socket.close();
                        }catch (IOException e) {
                            Log.e("ClienTask","Exception caught");
                            e.printStackTrace();
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                }
            }

            if (pstr[0].equals("find_replicas")) {
                StringBuilder stringBuilder = new StringBuilder();
                String key_succ;
                String val_succ = "";
                String file;

                BlockingQueue queryqueue = (ArrayBlockingQueue<String>) msgs[1];
                file = pstr[1];
                Log.d("pstr[2]",pstr[2]);
                int query_counter = Integer.parseInt(pstr[2]);
                String hash = "";
                try {
                    hash = genHash(file);
                } catch (Exception e) {
                }
                ArrayList<String> needed_ports = return_port(hash);
                reset();

                if (query_counter == 1) {
                    needed_ports.remove(c_port);
                }
                String[] np_array = needed_ports.toArray(new String[needed_ports.size()]);

                for (int i = 0; i < np_array.length; i++) {
                    try {
                        remotePort = avd_id.get(np_array[i]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        Log.d("send to avd: ", np_array[i]);
                        String msgToSend = "query_replicas" + ":" + file;
                        msgToSend = msgToSend.trim();
                        Log.d("Client", "Message to be sent:" + msgToSend);
                        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                        dout.writeUTF(msgToSend);
                        dout.flush();
                        Log.d("Client","Message flushed");
                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        String message = din.readUTF();
                        Log.d("Client","Message recieved"+message);
                        if(message != null){
                        Log.d("mesg",message);
                        String[] m0 = message.split("\\$");
                        if (m0[0].equals("result")) {
                            Log.d("Client", "Query Replica Msg sent to succ " + np_array[i]);
                            if (!m0[1].equals("nullcursor")) {
                                key_succ = m0[1];
                                val_succ = m0[2];
                                stringBuilder.append(val_succ);
                                stringBuilder.append("$");
                                }
                            }
                        }
                        socket.close();
                        } catch (Exception e) {
                        Log.e("ClientQuery", "Caught an exception");
                    }
                }

                String final_value = stringBuilder.toString();
                Log.d("final val","final value"+final_value);
                try {
                    queryqueue.put(final_value);
                }catch (InterruptedException i){}
            }



            if(pstr[0].equals("*query")){
                StringBuilder string1 = new StringBuilder();
                for (int i = 0; i < rest.size(); i++){
                        try {
                            remotePort = avd_id.get(rest.get(i));
                            Log.d("remoteport",remotePort);
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            String msgToSend = "query*:" + c_port;
                            msgToSend = msgToSend.trim();
                            Log.d("Client", "Query Message hopped to:" + msgToSend);
                            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                            dout.writeUTF(msgToSend);
                            dout.flush();

                            DataInputStream din = new DataInputStream(socket.getInputStream());
                            String ack = din.readUTF();
                            if(ack != null) {
                                String[] m1 = ack.split("\\$");
                                if(m1.length == 2){
                                string1.append(m1[1]);}
                                }

                            socket.close();
                        }catch (Exception e) {
                            Log.e("Client", "ClientTask socket IOException");
                            e.printStackTrace();
                        }
                }
                if(string1.toString() == null){
                    string1.append("nullcursor");
                }
                try{
                    Log.d("String",string1.toString());
                squeue.put(string1.toString());}catch (InterruptedException i){}
              }
            if(pstr[0].equals("*del*")){
                for (int i = 0; i < rest.size(); i++){
                        try {
                            remotePort = avd_id.get(rest.get(i));
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            String msgToSend = "*del:" + c_port;
                            msgToSend = msgToSend.trim();
                            Log.d("Client", "Delete Message-*-hopped is:" + msgToSend);
                            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                            dout.writeUTF(msgToSend);
                            dout.flush();

                            DataInputStream din = new DataInputStream(socket.getInputStream());
                            String ack = din.readUTF();
                            if(ack != null) {
                                if (ack.equals("ack")) {
                                    Log.d("Client", "Deleted from node " + ports[i]);
                                    socket.close();
                                }
                            }
                            else {
                                socket.close();
                            }
                        }catch (Exception e) {
                            Log.e("Client", "ClientTask socket IOException");
                            e.printStackTrace();
                        }
                    }
                }

              if(pstr[0].equals("del_specific")){
                String file = pstr[1];
                String file_hash = "";
                try {
                     file_hash = genHash(file);
                    }catch (Exception e){}

                ArrayList<String> needed_ports = return_port(file_hash);
                reset();

                String[] np_array = needed_ports.toArray(new String[needed_ports.size()]);

                for(int i = 0; i < np_array.length; i++) {
                    try {
                        remotePort = avd_id.get(np_array[i]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        Log.d("send to avd: ", np_array[i]);
                        String msgToSend = "del_replicas" + ":" + file;
                        msgToSend = msgToSend.trim();
                        Log.d("Client", "Message to be sent:" + msgToSend);
                        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                        dout.writeUTF(msgToSend);
                        dout.flush();

                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        String message = din.readUTF();
                        if(message != null) {
                            if (message.equals("ack")) {
                                Log.d("Client", "Deleted at " + np_array[i]);
                                socket.close();
                            }
                        }
                        else {socket.close();}
                    }catch (Exception e) {
                        Log.d("ClientTask","Exception caught");
                    }
            }
          }

            return null;
        }
    }
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
        if(thread_flag == false){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //Initialization//
        MatrixCursor cursor = null;
        String final_s;
        String h_key = "";
        String str_final;
        FileInputStream fis;
        InputStreamReader isr;
        BufferedReader br;

        try {
                h_key = genHash(selection);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        Log.d("Query", "Selection_Value"+selection);
        Log.d("Query","Hash to be queried: "+h_key);

        if(selection.equals("@") && selectionArgs == null){
            try{
                    String imsg = "@query"+":"+"useless";
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
                    squeue1 = new ArrayBlockingQueue<String>(1);
                    String final_sq = squeue1.take();
                    if(final_sq.equals("nullcursor")){
                        cursor = null;
                    }
                    else {
                        Log.d("query:", "Blocking queue populated with:" + final_sq);
                        str_final = final_sq;
                        Log.d("final", str_final);
                        String[] pair = str_final.split("\\|");
//                    Log.d("pair length", "" + pair.length);
//                    for (int i = 0; i < pair.length; i++) {
//                        Log.d("=", pair[i]);
//                    }
                        cursor = new MatrixCursor(new String[]{"key", "value"});
                        for (int i = 0; i < pair.length; i++) {
                            Log.d("pair", pair[i]);
                            String[] key_val = pair[i].split("\\*");
                            String key = key_val[0];
                            String val = key_val[1];
                            String[] splitval = val.split("\\!");
//                        Log.d("pairval:", key + " " + val);
                            cursor.addRow(new String[]{key, splitval[0]});
                            Log.d("row", "added" + i);
                        }
                        Log.d("cursor rows:", "" + cursor.getCount());
                    }
            }catch (Exception e){
                Log.e("@Query","Caught Exception!");
            }
        }

         else if(selection.equals("@") && selectionArgs != null){
             try {
                File dir = getContext().getFilesDir();
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    if(files != null){
                    cursor = new MatrixCursor(new String[]{"key", "value"});
                    Log.d("**Query**","Total number of files : "+files.length);
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        String strFileName = file.getName();
                        Log.d("**Query**","File : "+strFileName);
                        Log.d("**Query**","hash:"+genHash(strFileName));
                        String val = queryforresult(strFileName);
                        Log.d("checking val",val);
                        cursor.addRow(new String[]{strFileName, val});
                        Log.d("**Query**","Total number of rows : "+files.length);
                        if(i == files.length - 1){
                            if(cursor.getCount() == files.length){
                                Log.d("**Query**","Success");
                                }
                            }
                        }
                    }
                    else {cursor = null;}
                }
            }catch (Exception e)
            {
            Log.d("**Query**","Caught IOException");
            }
         }
         else if(selection.equals("*")) {
             String add_str = "";
            try {
                File dir = getContext().getFilesDir();
                File[] files = dir.listFiles();
                Log.d("**GDump**", "Total number of files : " + files.length);
                if(files != null) {
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        String strFileName = file.getName();
                        Log.d("**GDump**", "File : " + strFileName);
                        String result = queryforresult(strFileName);
                        String res[] = result.split("\\!");//splitting timestamp
                        result = res[0];
                        Log.d("**GDump**", ":" + result);
                        add_str = strFileName + "*" + result + "|";
                        Log.d("GDump", add_str);
                        sbuf.append(add_str);
                    }
                }
                else{
                    sbuf.append(add_str);
                }
                    String imsg = "*query"+":"+c_port;
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, myPort);
                    squeue = new ArrayBlockingQueue<String>(1);
                    final_s = squeue.take();
                    if(final_s.equals("nullcursor")){
                        cursor = null;
                    }
                    else {
                        Log.d("query:", "Blocking queue populated with:" + final_s);
                        str_final = sbuf.toString() + final_s;
                        Log.d("final", str_final);
                        String[] pair = str_final.split("\\|");
//                    Log.d("pair length", "" + pair.length);
//                    for (int i = 0; i < pair.length; i++) {
//                        Log.d("=", pair[i]);
//                    }
                        cursor = new MatrixCursor(new String[]{"key", "value"});
                        for (int i = 0; i < pair.length; i++) {
                            Log.d("pair", pair[i]);
                            String[] key_val = pair[i].split("\\*");
                            String key = key_val[0];
                            String val = key_val[1];
                            String[] splitval = val.split("\\!");
//                        Log.d("pairval:", key + " " + val);
                            cursor.addRow(new String[]{key, splitval[0]});
                            Log.d("row", "added" + i);
                        }
                        Log.d("cursor rows:", "" + cursor.getCount());
                    }

            } catch (Exception e) {
                Log.d("**Query**", "Caught IOException");
            }
        }

        else {
             try {
                 Thread.sleep(100);
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
             try {
                 Log.d("query:", "current:" + c_port + "hash" + c_hash);
                 ArrayList<String> needed_ports = return_port(h_key);
                 reset();
                 Log.d("Charanya",needed_ports.get(0)+":"+needed_ports.get(1)+":"+needed_ports.get(2));
                 if (needed_ports.contains(c_port)) {
                     int query_counter = 0;
                     String all_values = "";
                     String val_curr = "useless";
                     BlockingQueue<String> queryqueue = new ArrayBlockingQueue<String>(1);
                     //Query current port for value and query for 2 replicas
                     if(file_exists(selection)) {
                         val_curr = queryforresult(selection);
                         query_counter++;
                         Log.d("val_curr%querycounter",val_curr+ " % "+query_counter);
                     }
                     String imsg = String.format("find_replicas:%s:%s", selection, String.valueOf(query_counter));//changes
                     new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, queryqueue);
                     String res = queryqueue.take();
                     Log.d("BQ","Blocking queue :"+res);
                     if(!val_curr.equals("useless")) {
                         all_values = val_curr + "$" + res;
                     }
                     else{all_values = res;}
                     Log.d("all_values",all_values);
                     String final_val = find_recentval(all_values);
                     Log.d("FV","final_val"+final_val);
                     cursor = new MatrixCursor(new String[]{"key", "value"});
                     cursor.addRow(new String[]{selection, final_val});
                 } else {
                     //Query coord and two replicas
                     int query_counter = 0;
                     BlockingQueue<String> queryqueue = new ArrayBlockingQueue<String>(1);
                     String imsg = String.format("find_replicas:%s:%s", selection,String.valueOf(query_counter));//changes
                     new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imsg, queryqueue);
                     String res = queryqueue.take();
                     Log.d("BQ","Blocking queue :"+res);
                     String all_values = res;
                     String final_val = find_recentval(all_values);
                     Log.d("FV","final_val"+final_val);
                     cursor = new MatrixCursor(new String[]{"key", "value"});
                     cursor.addRow(new String[]{selection, final_val});
                 }
             }catch (Exception e){
                 Log.e("Query", "Caught Exception");
                 e.printStackTrace();
             }
        }

		return cursor;
	}

    private String queryforresult(String selection) {
        String result = "";
        FileInputStream fis;
        InputStreamReader isr;
        BufferedReader br;
        try {
            fis = getContext().openFileInput(selection);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            result = br.readLine();
            Log.d("RES",result);
            br.close();
            isr.close();
            fis.close();
        }catch (Exception e){}
        return result;
    }


    @Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
