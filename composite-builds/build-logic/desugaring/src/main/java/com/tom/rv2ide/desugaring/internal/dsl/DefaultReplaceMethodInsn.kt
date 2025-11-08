package com.tom.rv2ide.desugaring.internal.dsl

import com.tom.rv2ide.desugaring.dsl.MethodOpcode
import com.tom.rv2ide.desugaring.dsl.ReplaceMethodInsn
import java.io.Serializable

/**
 * @author Akash Yadav
 */
data class DefaultReplaceMethodInsn(override var fromClass: String,
                                    override var methodName: String,
                                    override var methodDescriptor: String,
                                    override var requireOpcode: MethodOpcode?,
                                    override var toClass: String,
                                    override var toMethod: String,
                                    override var toMethodDescriptor: String,
                                    override var toOpcode: MethodOpcode
) : ReplaceMethodInsn, Serializable {

  @JvmField
  val serialVersionUID = 1L
}