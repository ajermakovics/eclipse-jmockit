package jmockit.assist;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class InvocationFilter implements IMethodBinding {
	IMethodBinding originalBinding;

	public InvocationFilter(IMethodBinding originalBinding) {
		this.originalBinding = originalBinding;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		return originalBinding.getAnnotations();
	}

	@Override
	public IJavaElement getJavaElement() {
		return originalBinding.getJavaElement();
	}

	@Override
	public String getKey() {
		return originalBinding.getKey();
	}

	@Override
	public int getKind() {
		return originalBinding.getKind();
	}

	@Override
	public int getModifiers() {
		return originalBinding.getModifiers();
	}

	@Override
	public boolean isDeprecated() {
		return originalBinding.isDeprecated();
	}

	@Override
	public boolean isEqualTo(IBinding arg0) {
		return originalBinding.isEqualTo(arg0);
	}

	@Override
	public boolean isRecovered() {
		return originalBinding.isRecovered();
	}

	@Override
	public boolean isSynthetic() {
		return originalBinding.isSynthetic();
	}

	@Override
	public ITypeBinding getDeclaringClass() {
		return originalBinding.getDeclaringClass();
	}

	@Override
	public Object getDefaultValue() {
		return originalBinding.getDefaultValue();
	}

	@Override
	public ITypeBinding[] getExceptionTypes() {
		return originalBinding.getExceptionTypes();
	}

	@Override
	public IMethodBinding getMethodDeclaration() {
		return originalBinding.getMethodDeclaration();
	}

	@Override
	public String getName() {
		return originalBinding.getName();
	}

	@Override
	public IAnnotationBinding[] getParameterAnnotations(int arg0) {
		return originalBinding.getParameterAnnotations(arg0);
	}

	@Override
	public ITypeBinding[] getParameterTypes() {
		ITypeBinding[] originalParams = originalBinding.getParameterTypes();
		if (originalParams.length > 0 && originalParams[0].getQualifiedName().equals("mockit.Invocation")) {
			ITypeBinding[] newParams = new ITypeBinding[originalParams.length - 1];
			for (int i = 1; i < originalParams.length; i++)
				newParams[i - 1] = originalParams[i];
			return newParams;
		}

		return originalBinding.getParameterTypes();
	}

	@Override
	public ITypeBinding getReturnType() {
		return originalBinding.getReturnType();
	}

	@Override
	public ITypeBinding[] getTypeArguments() {
		return originalBinding.getTypeArguments();
	}

	@Override
	public ITypeBinding[] getTypeParameters() {
		return originalBinding.getTypeParameters();
	}

	@Override
	public boolean isAnnotationMember() {
		return originalBinding.isAnnotationMember();
	}

	@Override
	public boolean isConstructor() {
		return originalBinding.isConstructor();
	}

	@Override
	public boolean isDefaultConstructor() {
		return originalBinding.isDefaultConstructor();
	}

	@Override
	public boolean isGenericMethod() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isParameterizedMethod() {
		return originalBinding.isParameterizedMethod();
	}

	@Override
	public boolean isRawMethod() {
		return originalBinding.isRawMethod();
	}

	@Override
	public boolean isSubsignature(IMethodBinding arg0) {
		return originalBinding.isSubsignature(arg0);
	}

	@Override
	public boolean isVarargs() {
		return originalBinding.isVarargs();
	}

	@Override
	public boolean overrides(IMethodBinding arg0) {
		return originalBinding.overrides(arg0);
	}

}
