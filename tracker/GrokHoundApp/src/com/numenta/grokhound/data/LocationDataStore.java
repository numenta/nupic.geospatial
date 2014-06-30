/**
 *
 * Copyright (C) 2014 Numenta Inc. All rights reserved.
 *
 * The information and source code contained herein is the
 * exclusive property of Numenta Inc.  No part of this software
 * may be used, reproduced, stored or distributed in any form,
 * without explicit written authorization from Numenta Inc.
 *
 */
package com.numenta.grokhound.data;

import java.io.IOException;
import java.util.Collection;

import android.location.Location;

public interface LocationDataStore {
	public void save(Collection<Location> locations) throws IOException;
}
