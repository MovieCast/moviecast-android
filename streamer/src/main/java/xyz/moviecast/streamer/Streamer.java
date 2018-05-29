package xyz.moviecast.streamer;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import xyz.moviecast.streamer.exceptions.InitializeFailedException;
import xyz.moviecast.streamer.listeners.AddTorrentAlertListener;
import xyz.moviecast.streamer.utils.TorrentUtils;

public class Streamer {

    private static final String TAG = "MOVIECAST_STREAMER";

    private static Streamer instance;

    public static Streamer getInstance() {
        if(instance == null) instance = new Streamer();
        return instance;
    }

    private CountDownLatch initLatch;

    private boolean streaming = false;

    private SessionManager torrentSession;

    // Threads
    private HandlerThread libTorrentThread;
    private Handler libTorrentHandler;

    private HandlerThread streamerThread;
    private Handler streamerHandler;

    private Streamer() {
        initLatch = new CountDownLatch(1);

        libTorrentThread = new HandlerThread("MOVIECAST_LIBTORRENT");
        libTorrentThread.start();
        libTorrentHandler = new Handler(libTorrentThread.getLooper());
        libTorrentHandler.post(() -> {
            torrentSession = new SessionManager();

            torrentSession.addListener(new AddTorrentAlertListener() {
                @Override
                public void onAddedTorrent(AddTorrentAlert alert) {
                    Log.d(TAG, "New torrent was added");
                }
            });

            torrentSession.start();

            Timer initTimer = new Timer();
            initTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long nodes = torrentSession.stats().dhtNodes();
                    if(nodes >= 10) {
                        Log.d(TAG, "Initialized, DHT contains " + nodes + " nodes");
                        initLatch.countDown();
                        initTimer.cancel();
                    }
                }
            }, 0, 1000);

            try {
                Log.d(TAG, "Waiting for nodes in DHT (10 seconds)...");
                boolean success = initLatch.await(10, TimeUnit.SECONDS);

                if(!success) {
                    throw new InitializeFailedException("DHT bootstrap timeout, it looks like DHT's are being blocked");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void start(String hash) {
        if(streaming) return;

        streamerThread = new HandlerThread("MOVIECAST_STREAMER");
        streamerThread.start();
        streamerHandler = new Handler(streamerThread.getLooper());

        streamerHandler.post(() -> {
            streaming = true;

            try {
                initLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                streaming = false;
            }

            TorrentInfo info = TorrentUtils.getTorrentInfo(torrentSession, hash);

            Log.d(TAG, "Torrent Name: " + info.name());
            Log.d(TAG, "Torrent Amount Files: " + info.files().numFiles());

            // TODO: Write the actual logic...

            torrentSession.download(info, new File("/"));
        });
    }

    public void pause() {
        libTorrentHandler.post(torrentSession::pause);
    }

    public void resume() {
        // First make sure all callbacks and messages get canceled.
        libTorrentHandler.removeCallbacksAndMessages(null);

        if(torrentSession.isPaused()) {
            libTorrentHandler.post(torrentSession::resume);
        }
    }

    public void stop() {
        if(!streaming) return;

        libTorrentHandler.removeCallbacksAndMessages(null);
        streamerHandler.removeCallbacksAndMessages(null);

        streaming = false;

        // TODO: Write torrent logic over here

        streamerThread.interrupt();

    }
}