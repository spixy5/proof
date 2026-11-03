package com.proofapp.android.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.proofapp.android.ProofApplication
import com.proofapp.android.di.AppContainer

/** Gives ViewModel initializers access to the dependency container. */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as ProofApplication).container
