package httpproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.sound.sampled.Port;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class proxyHttpClient {
    private String ip;
    private int port;
    private String path;
    private boolean isSSL;
    private String data;

    public proxyHttpClient(String ip, int port, String path, boolean isSSL, String data) {
        this.ip = ip;
        this.port = port;
        this.path = path;
        this.isSSL = isSSL;
        this.data = data;
    }

    public static void main(String[] args) {
        proxyHttpClient test = new proxyHttpClient("192.168.0.2", 10000, "", false, "{\'test\':\'test\'}");
        test.send();
    }
    public String send(){
        final String[] rcvCompleteData = new String[1];

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new SimpleChannelInboundHandler<HttpObject>() {
                    String rcvData="";

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                        if (msg instanceof HttpContent) {
                            HttpContent content = (HttpContent) msg;
                            rcvData += content.content().toString(CharsetUtil.UTF_8);
                            if(msg instanceof LastHttpContent){

                                rcvCompleteData[0] = rcvData;
                                ctx.close();
                            }
                        }
                    }
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        cause.printStackTrace();
                        ctx.close();
                    }
                });
            }
        });
        String URL = "";
        if(isSSL){
               URL+= "https://";
        }else{
            URL+= "http://";

        }
        URL += ip+":"+Integer.toString(port)+path;
        try {
            URI uri = new URI(URL);
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getRawPath());
            request.headers().set(HttpHeaderNames.HOST, uri.getHost());
            request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            ByteBuf byteBuf = Unpooled.copiedBuffer(data, StandardCharsets.UTF_8);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
            request.content().writeBytes(byteBuf);

            Channel ch = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
            ch.writeAndFlush(request);
            ch.closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

        System.out.println("========proxyClient response==== \n"+rcvCompleteData[0]);
        return rcvCompleteData[0];
    }
}