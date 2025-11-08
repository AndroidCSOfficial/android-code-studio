#ifndef ANDROIDGLINVESTIGATIONS_ANDROIDOUT_H
#define ANDROIDGLINVESTIGATIONS_ANDROIDOUT_H

#include <android/log.h>
#include <sstream>

/*!
 * Use this to log strings out to logcat. Note that you should use std::endl to commit the line
 *
 * ex:
 *  aout << "Hello World" << std::endl;
 */
extern std::ostream aout;

/*!
 * Use this class to create an output stream that writes to logcat. By default, a global one is
 * defined as @a aout
 */
class AndroidOut: public std::stringbuf {
public:
    /*!
     * Creates a new output stream for logcat
     * @param kLogTag the log tag to output
     */
    inline AndroidOut(const char* kLogTag) : logTag_(kLogTag){}

protected:
    virtual int sync() override {
        __android_log_print(ANDROID_LOG_DEBUG, logTag_, "%s", str().c_str());
        str("");
        return 0;
    }

private:
    const char* logTag_;
};

#endif //ANDROIDGLINVESTIGATIONS_ANDROIDOUT_H