/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.inputmethod;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for safe parcelable list wrappers.
 *
 * <p>This class provides a secure way to pass lists of Parcelable objects across IPC
 * boundaries by marshalling them to a byte blob. This prevents type confusion attacks
 * that could occur with direct Parcelable list passing.</p>
 *
 * @param <T> The type of Parcelable elements in the list
 * @see Parcel#readBlob()
 * @see Parcel#writeBlob(byte[])
 * @hide
 */
public abstract class AbstractSafeList<T extends Parcelable> implements Parcelable {

    @Nullable
    private byte[] mBuffer;

    /**
     * Creates an AbstractSafeList from a list of Parcelable objects.
     *
     * @param list The list to wrap, or null for an empty list
     */
    protected AbstractSafeList(@Nullable List<T> list) {
        if (list != null && !list.isEmpty()) {
            mBuffer = marshall(list);
        }
    }

    /**
     * Creates an AbstractSafeList from a marshalled byte buffer.
     *
     * @param buffer The marshalled byte buffer
     */
    protected AbstractSafeList(@Nullable byte[] buffer) {
        mBuffer = buffer;
    }

    /**
     * Extracts the list from an AbstractSafeList and clears the internal buffer.
     *
     * <p>This method consumes the buffer - subsequent calls will return an empty list.</p>
     *
     * @param from The AbstractSafeList to extract from
     * @param creator The Parcelable.Creator for the element type
     * @return The extracted list, or an empty list if the input was null/empty
     */
    @NonNull
    protected static <T extends Parcelable> List<T> extractFrom(
            @Nullable AbstractSafeList<T> from,
            @NonNull Parcelable.Creator<T> creator) {
        if (from == null) {
            return new ArrayList<>();
        }
        final byte[] buf = from.mBuffer;
        from.mBuffer = null;
        if (buf != null) {
            final List<T> list = unmarshall(buf, creator);
            if (list != null) {
                return list;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBlob(mBuffer);
    }

    /**
     * Marshalls a list of Parcelables to a byte array.
     *
     * @param list The list to marshall
     * @return The marshalled byte array
     */
    @Nullable
    protected static <T extends Parcelable> byte[] marshall(@NonNull List<T> list) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parcel.writeTypedList(list);
            return parcel.marshall();
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }

    /**
     * Unmarshalls a byte array to a list of Parcelables.
     *
     * @param data The byte array to unmarshall
     * @param creator The Parcelable.Creator for the element type
     * @return The unmarshalled list
     */
    @Nullable
    protected static <T extends Parcelable> List<T> unmarshall(
            @NonNull byte[] data,
            @NonNull Parcelable.Creator<T> creator) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            return parcel.createTypedArrayList(creator);
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }
}
