package com.example.hapticogg.export

import android.net.Uri
import android.provider.DocumentsContract

/** The two document URIs that matter for a folder the user picked through SAF. */
internal object DocumentTrees {

    /** URI of the folder itself. */
    fun folder(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )

    /** URI that lists the children of the folder. */
    fun children(treeUri: Uri): Uri =
        DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )
}
