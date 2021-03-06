﻿using De.Osthus.Ambeth.Annotation;
using System;

namespace De.Osthus.Ambeth.Security.Config
{
    [ConfigurationConstants]
    public sealed class RESTConfigurationConstants
    {
        public const String HttpUseClient = "rest.http.client";

        public const String HttpAcceptEncodingZipped = "rest.http.accept-encoding.zipped";

        public const String HttpContentEncodingZipped = "rest.http.content-encoding.zipped";

        private RESTConfigurationConstants()
        {
            // intended blank
        }
    }
}
