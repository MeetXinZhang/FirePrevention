package cn.devin.fireprevention.Presenter;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import cn.devin.fireprevention.DetailContract;
import cn.devin.fireprevention.Model.MyLatLng;
import cn.devin.fireprevention.Tools.ParseData;

/**
 * Created by Devin on 2015/12/27.
 * TCP 协议封装类
 */

public class TCPPresenter implements Runnable, DetailContract.TCPPre{
    private static String TAG = "TCPPresenter";

    private DetailContract.MainServ mainServ;

    private String serverAddress = "172.22.98.45";
    private int port = 1988;
    private Socket socket;

    private BufferedReader br = null;
    private BufferedWriter bw = null;
//    private DataOutputStream outputStream = null;
//    private DataInputStream inputStream = null;

    public TCPPresenter(DetailContract.MainServ mainServ, String ip, int port){
        this.mainServ = mainServ;
        if (ip!=null && port!=0){
            this.serverAddress = ip;
            this.port = port;
        }
    }

    /**
     * 接受消息：
     * 1-队友位置， 2-火情分布，
     * 3-新任务：两个坐标都设置为0，表示任务完成
     * 0-聊天信息， 8-登录信息
     */
    @Override
    public void run() {
        try {
            socket = new Socket(serverAddress, port);
            //设置客户端与服务器建立连接的超时时长为30秒
            //socket.setSoTimeout(30000);
            //初始化缓存
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
            this.bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"utf-8"));
//            outputStream = new DataOutputStream(socket.getOutputStream());
//            inputStream = new DataInputStream(socket.getInputStream());

            //通知后台服务：连接已成功建立
            //mainServ.onConnectSuccess();

            // 从InputStream当中读取客户端所发送的数据
            String s = null;
//            Object obj;
            while ((s = br.readLine())!=null) {

                switch (s.charAt(0)){
                    case '1':
                        mainServ.onTeamChange(ParseData.getTeam(s.substring(1)));
                        break;
                    case '2':
                        mainServ.onFireChange(ParseData.getFire(s.substring(1)));
                        break;
                    case '3':
                        mainServ.onTaskChange(ParseData.getTask(s.substring(1)));
                        break;
                    case '0':
                        mainServ.onChatChange(s.substring(1));
                        break;
                    case '8':
                        if (s.substring(1).equals('1')){
                            mainServ.onConnectSuccess(true);
                        }else {
                            mainServ.onConnectSuccess(false);
                        }
                        break;
                    default:
                        Log.d(TAG, "run: + 收到的消息没有首位类型标记！");
                        break;
                }
            }
            //br.close();
        }catch(IOException e){
            e.printStackTrace();
            mainServ.onConnectSuccess(false);
        }
    }


    /**
     * 发送位时考虑到要重复调用，需要调用方自带线程
     * @param myLatLng 经纬度位置
     * @param type 位置类型：1-我的位置， 2-着火位置， 3-已灭火位置
     */
    @Override
    public void sendMyLatlng(MyLatLng myLatLng, final int type) {
        Gson gson = new Gson();
        final String json = gson.toJson(myLatLng);

        try {
//            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"utf-8"));
//            outputStream.writeBytes(type + json +"\n");
//            outputStream.flush();
            if (bw != null){
                bw.write(type + json +"\n");
                bw.flush();
            }else {
                mainServ.onConnectSuccess(false);
            }

        } catch (IOException e){
            //TODO Auto-generated catch block
            e.printStackTrace();
            mainServ.onConnectSuccess(false);
        }
    }


    /**
     * 发送登录，聊天信息
     * 0-聊天信息， 8-登录信息
     * @param message
     * @param type
     */
    public void sendString(final String message, final int type){
        if (bw==null){
            mainServ.onConnectSuccess(false);
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        bw.write(type + message+"\n");
                        bw.flush();

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        mainServ.onConnectSuccess(false);
                    }
                }
            }).start();

        }
    }

}
