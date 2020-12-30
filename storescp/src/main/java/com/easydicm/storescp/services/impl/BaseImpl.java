package com.easydicm.storescp.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 *
 * @author dhz
 */
public abstract class BaseImpl {
    protected final Logger LOG;

    protected BaseImpl() {
        LOG = LoggerFactory.getLogger(this.getClass());
    }
}
