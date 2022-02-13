package com.atxiaojie.chatsystem;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * @ClassName: GroupChatServer
 * @Description: 群聊服务端
 * @author: zhouxiaojie
 * @date: 2022/1/14 18:41
 * @Version: V1.0.0
 */
public class GroupChatServer {

    //定义属性
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    //构造器，初始化
    public GroupChatServer() {
        try {
            //得到selector
            selector = Selector.open();
            //得到ServerSocketChannel
            listenChannel = ServerSocketChannel.open();
            //绑定端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            //开启非阻塞
            listenChannel.configureBlocking(false);
            //把listenChannel注册到selector
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //监听
    public void listen(){
        try {
            while (true){
                int count = selector.select();
                if(count > 0){
                    //有事件处理
                    //遍历得到selectionKey集合
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()){
                        //取出selectionKey
                        SelectionKey key = iterator.next();
                        if(key.isAcceptable()){
                            SocketChannel sc = listenChannel.accept();
                            sc.configureBlocking(false);
                            //将该sc注册到selector
                            sc.register(selector, SelectionKey.OP_READ);
                            //提示上线了
                            System.out.println(sc.getRemoteAddress() + "上线了...");
                        }
                        if(key.isReadable()){
                            //通道发生read事件，通道是可读的状态，处理读
                            readData(key);
                        }
                        //当前的key删除，房子重复处理
                        iterator.remove();
                    }
                }else{
                    System.out.println("等待中....");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }
    }

    //读取客户端消息
    private void readData(SelectionKey key){
        //定义SocketChannel
        SocketChannel channel = null;
        try {
            //得到channel
            channel = (SocketChannel) key.channel();
            //创建一个buffer，接受读的数据
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channel.read(buffer);
            if(count > 0){
                String msg = new String(buffer.array());
                System.out.println("from 客户端：" + msg);
                //向其它客户端转发消息,排除自己
                sendInfoToOtherClient(msg, channel);
            }
        }catch (IOException e){
            try {
                System.out.println(channel.getRemoteAddress() + "离线了...");
                //取消注册
                key.cancel();
                //关闭通道
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //服务器转发消息中
    private void sendInfoToOtherClient(String msg, SocketChannel self) throws IOException {
        System.out.println("服务器转发消息中...");
        //遍历所有注册到selector上的SocketChannel，并排除自己
        for(SelectionKey key : selector.keys()){
            //通过key取对应的socketChannel
           Channel targetChannel = key.channel();
           //排除自己
            if(targetChannel instanceof SocketChannel && targetChannel != self){
                //转成SocketChannel
                SocketChannel dest = (SocketChannel) targetChannel;
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                dest.write(buffer);
            }
        }
    }

    public static void main(String[] args) {
        //创建一个服务器对象
        GroupChatServer groupChatServer = new GroupChatServer();
        groupChatServer.listen();
    }

}
