package com.example.muyinteresanteNoTocar;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.net.ssl.SSLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.example.muyinteresante.util.ConnectivityAndInternetAccess;
import com.example.muyinteresante.util.RemoteRequestPolicy;

/* Parsea un canal RSS y devuelve sus items en un ArrayList. */
public class DescargaNoticiasRSS extends AsyncTask<String, Integer, ArrayList<NoticiaRSS>> {
    private static final String TAG = "DescargaNoticiasRSS";
    private static final String MENSAJE_PD = "Descargando noticias...";

    public enum FailureKind {
        OFFLINE_GUARD,
        HTTP_RESPONSE,
        AMBIGUOUS_CONNECTIVITY,
        FEED_UNAVAILABLE,
        GENERAL_CONNECTIVITY_UNAVAILABLE,
        OTHER
    }

    public static final class DownloadFailure {
        private final FailureKind kind;
        private final int httpStatus;
        private final Throwable cause;
        private final Boolean generalInternetReachable;

        private DownloadFailure(
                FailureKind kind,
                int httpStatus,
                Throwable cause,
                Boolean generalInternetReachable) {
            this.kind = kind;
            this.httpStatus = httpStatus;
            this.cause = cause;
            this.generalInternetReachable = generalInternetReachable;
        }

        public FailureKind getKind() {
            return kind;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public Throwable getCause() {
            return cause;
        }

        public Boolean getGeneralInternetReachable() {
            return generalInternetReachable;
        }

        private DownloadFailure withGeneralInternetReachable(boolean reachable) {
            RemoteRequestPolicy.FailureClassification classification =
                    RemoteRequestPolicy.classifyAmbiguousFailure(reachable);
            return new DownloadFailure(
                    classification == RemoteRequestPolicy.FailureClassification.FEED_UNAVAILABLE
                            ? FailureKind.FEED_UNAVAILABLE
                            : FailureKind.GENERAL_CONNECTIVITY_UNAVAILABLE,
                    httpStatus,
                    cause,
                    reachable);
        }
    }

    private final Context contexto;
    private final iNoticiaRSS objetoReceptor;
    private final boolean mostrarProgreso;
    private ProgressDialog pd;
    private DownloadFailure failure;
    private boolean receivedHttpResponse;

    public DescargaNoticiasRSS(Context contexto, iNoticiaRSS objetoReceptor) {
        this(contexto, objetoReceptor, true);
    }

    /**
     * Used by pagination so older pages do not open a modal progress dialog.
     */
    public DescargaNoticiasRSS(
            Context contexto,
            iNoticiaRSS objetoReceptor,
            boolean mostrarProgreso) {
        this.contexto = contexto;
        this.objetoReceptor = objetoReceptor;
        this.mostrarProgreso = mostrarProgreso;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (contexto != null) {
            ConnectivityAndInternetAccess.beginConnectionAttempt(contexto);
        }

        if (mostrarProgreso && contexto != null) {
            pd = new ProgressDialog(contexto);
            pd.setMessage(MENSAJE_PD);
            pd.setCancelable(true);
            pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    DescargaNoticiasRSS.this.cancel(true);
                }
            });
            pd.show();
        }
    }

    @Override
    protected ArrayList<NoticiaRSS> doInBackground(String... params) {
        if (contexto != null
                && !RemoteRequestPolicy.shouldStartRequest(
                ConnectivityAndInternetAccess.isConnected(contexto))) {
            failure = new DownloadFailure(
                    FailureKind.OFFLINE_GUARD,
                    0,
                    null,
                    false);
            return null;
        }

        if (params == null || params.length < 2 || params[0] == null || params[0].trim().isEmpty()) {
            failure = new DownloadFailure(FailureKind.OTHER, 0, null, null);
            return null;
        }

        InputStream entrada = null;
        HttpURLConnection conexion = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setCoalescing(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            URL url = new URL(params[0]);
            URLConnection rawConnection = url.openConnection();
            rawConnection.setConnectTimeout(10000);
            rawConnection.setReadTimeout(10000);
            rawConnection.setUseCaches(false);
            rawConnection.setRequestProperty(
                    "Accept",
                    "application/rss+xml, application/xml, text/xml, */*");
            rawConnection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android) noticias-marca/1.2");
            if (!(rawConnection instanceof HttpURLConnection)) {
                failure = new DownloadFailure(FailureKind.OTHER, 0, null, null);
                return null;
            }

            conexion = (HttpURLConnection) rawConnection;
            conexion.setInstanceFollowRedirects(true);

            int responseCode = conexion.getResponseCode();
            receivedHttpResponse = true;
            if (responseCode < HttpURLConnection.HTTP_OK
                    || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                failure = new DownloadFailure(
                        FailureKind.HTTP_RESPONSE,
                        responseCode,
                        null,
                        null);
                return null;
            }

            entrada = conexion.getInputStream();
            Document arbolXML = db.parse(entrada);
            Element raiz = arbolXML.getDocumentElement();
            raiz.normalize();

            ArrayList<NoticiaRSS> noticias = new ArrayList<NoticiaRSS>();
            NodeList listaItems = raiz.getElementsByTagName("item");
            for (int i = 0; i < listaItems.getLength(); i++) {
                try {
                    Element item = (Element) listaItems.item(i);
                    noticias.add(new NoticiaRSS(item, params[1]));
                    publishProgress(noticias.size());
                } catch (Exception itemError) {
                    Log.w(TAG, "Se omitió un item RSS inválido", itemError);
                }
            }
            return noticias;
        } catch (Exception error) {
            boolean ambiguous = isAmbiguousConnectivityFailure(error);
            if (RemoteRequestPolicy.shouldDiagnoseAfterFailure(
                    receivedHttpResponse,
                    ambiguous)) {
                failure = new DownloadFailure(
                        FailureKind.AMBIGUOUS_CONNECTIVITY,
                        0,
                        error,
                        null);
            } else {
                failure = new DownloadFailure(
                        receivedHttpResponse
                                ? FailureKind.OTHER
                                : FailureKind.OTHER,
                        0,
                        error,
                        null);
            }
            Log.w(TAG, "Falló la petición RSS directa", error);
            return null;
        } finally {
            if (entrada != null) {
                try {
                    entrada.close();
                } catch (Exception ignored) {
                }
            }
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private boolean isAmbiguousConnectivityFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof NoRouteToHostException
                    || current instanceof SocketException
                    || current instanceof SSLException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Override
    protected void onPostExecute(ArrayList<NoticiaRSS> result) {
        super.onPostExecute(result);
        ConnectivityAndInternetAccess.endConnectionAttempt();
        if (pd != null) {
            pd.dismiss();
        }

        if (result != null) {
            if (objetoReceptor != null) {
                objetoReceptor.onRecibeNoticiasRSS(result);
            }
            return;
        }

        if (failure != null && failure.getKind() == FailureKind.AMBIGUOUS_CONNECTIVITY) {
            ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                    contexto,
                    new ConnectivityAndInternetAccess.InternetCallback() {
                        @Override
                        public void onResult(
                                ConnectivityAndInternetAccess.InternetResult result) {
                            boolean reachable = result != null && result.isReachable();
                            DownloadFailure classified = failure.withGeneralInternetReachable(reachable);
                            if (objetoReceptor != null) {
                                objetoReceptor.onError(classified);
                            }
                        }
                    });
            return;
        }

        if (objetoReceptor != null) {
            objetoReceptor.onError(failure != null
                    ? failure
                    : new DownloadFailure(FailureKind.OTHER, 0, null, null));
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        ConnectivityAndInternetAccess.endConnectionAttempt();
        if (pd != null) {
            pd.dismiss();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (pd != null && values != null && values.length > 0) {
            pd.setMessage(MENSAJE_PD + " " + values[0]);
        }
    }
}
