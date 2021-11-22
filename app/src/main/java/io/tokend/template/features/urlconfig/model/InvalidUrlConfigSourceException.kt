package io.tokend.template.features.urlconfig.model

class InvalidUrlConfigSourceException(
    source: String?
) : IllegalArgumentException("An UrlConfig can't be obtained from the source string: $source")