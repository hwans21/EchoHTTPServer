import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;

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
                            SslContext sslCtx = getCertificate();
                            if (sslCtx != null) {
                                ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                            }
                        }
                        //HttpServerCodec is a helper ChildHandler that encompasses
                        //both HTTP request decoding and HTTP response encoding
                        ch.pipeline().addLast(new HttpServerCodec());
                        //HttpObjectAggregator helps collect chunked HttpRequest pieces into
                        //a single FullHttpRequest. If you don't make use of streaming, this is
                        //much simpler to work with.
                        ch.pipeline().addLast(new HttpObjectAggregator(1048576));
                        //Finally add your FullHttpRequest handler. Real examples might replace this
                        //with a request router
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                ctx.flush();
                                //The close is important here in an HTTP request as it sets the Content-Length of a
                                //response body back to the client.
                                ctx.close();
                            }
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, msg.content().copy());
                                ctx.write(response);
                            }
                        });
                    }
                });

        // Start the server & bind to a random port.
        return b.bind();
    }
    private static SslContext getCertificate() throws SSLException {
        SslContext sslContext;
        try{
            File cert = new File("./keyfile/netty.crt");
            File key = new File("privatekey.pem");
            sslContext = SslContextBuilder.forServer(cert, key).build();
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Certification or Key path is null");
            sslContext = null;
        }
        return sslContext;

    }
}
