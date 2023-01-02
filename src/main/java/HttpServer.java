import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.Security;

public class HttpServer {
    public static void main(String[] args) throws Exception {
        if(args.length != 2){
            System.out.println("java -jar EchoHTTPSample-1.0-SNAPSHOT.jar (HTTP/HTTPS) PORTNUM");
            System.exit(0);
        }
        String HTTPOption = args[0];
        int port = Integer.parseInt(args[1]);
        System.out.println(HTTPOption+ port);
        EventLoopGroup serverWorkgroup = new NioEventLoopGroup();
        try{
            Channel serverChannel = HttpServer.server(serverWorkgroup, port, HTTPOption).sync().channel();
            serverChannel.closeFuture().sync();
        }finally {
            //Gracefully shutdown both event loop groups
            serverWorkgroup.shutdownGracefully();
        }
    }
    public static ChannelFuture server(EventLoopGroup workerGroup, int port, String HTTPOption) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(workerGroup).channel(NioServerSocketChannel.class)
                //Setting InetSocketAddress to port 0 will assign one at random
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        if(HTTPOption.equalsIgnoreCase("HTTPS")){
                            SSLContext sslCtx = getCertificate();
                            if (sslCtx != null) {
                                final SslHandler sslHandler =new SslHandler(sslCtx.createSSLEngine());
                                ch.pipeline().addLast("ssl",sslHandler);
                            }
                        }

                        ch.pipeline().addLast(new HttpServerCodec());

                        ch.pipeline().addLast(new HttpObjectAggregator(1048576));

                        ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                ctx.flush();

                                ctx.close();
                            }
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, msg.content().copy());
                                System.out.println(msg.content().toString(CharsetUtil.UTF_8));
                                ctx.write(response);
                            }
                        });
                    }
                });

        // Start the server & bind to a random port.
        return b.bind();
    }
    private static SSLContext getCertificate() throws SSLException {

        SSLContext sslContext = null;
        KeyStore ks = null;
        ByteArrayInputStream bsi =null;

        try {
            ks = KeyStore.getInstance("JKS");
            sslContext = SSLContext.getInstance("TLSv1.1");
            File file = new File("../certs/test.jks");
            byte[] temp = Files.readAllBytes(file.toPath());
            bsi = new ByteArrayInputStream(temp);
            ks.load(bsi,"test1234".toCharArray());
            String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, "test1234".toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
            tmf.init(ks);
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(),null);

        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                bsi.close();
            } catch (IOException e) {

            }
        }




        return sslContext;
    }
}
