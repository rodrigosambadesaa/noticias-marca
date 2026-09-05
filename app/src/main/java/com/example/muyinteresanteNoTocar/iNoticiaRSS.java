package com.example.muyinteresanteNoTocar;

import java.util.ArrayList;

public interface iNoticiaRSS {
	void onRecibeNoticiasRSS(ArrayList<NoticiaRSS> listaNoticias);

	default void onError(DescargaNoticiasRSS.DownloadFailure failure) {
		// Existing consumers may only need the successful RSS callback.
	}
}
