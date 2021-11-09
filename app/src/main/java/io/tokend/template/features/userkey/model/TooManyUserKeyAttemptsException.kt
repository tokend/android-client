package io.tokend.template.features.userkey.model

class TooManyUserKeyAttemptsException(val maxAttempts: Int) :
    Exception("Maximum failed attempts count is $maxAttempts")